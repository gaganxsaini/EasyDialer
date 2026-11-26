package com.example.easydialer

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.easydialer.data.ContactCard
import com.example.easydialer.data.ContactViewModel
import com.example.easydialer.data.ListAdapter


class MainActivity : AppCompatActivity() {
    private lateinit var mContactViewModel: ContactViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ListAdapter

    // After a tap starts a call, further taps are ignored until the call screen opens (or a few
    // seconds pass), so she can't start a second call while the phone is still catching up.
    private var ignoreTapsUntil = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val endCallingFeedback = Runnable {
        ignoreTapsUntil = 0L
        adapter.clearCalling()
    }

    // As the home screen, Back has nowhere to go: swallow it instead of closing the app.
    private val blockBackAsHome = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = Unit
    }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Names and photos can be read now (or still can't); show what we have either way.
            mContactViewModel.refresh()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter = ListAdapter(onCall = ::call, onLongPress = ::openSetup)
        adapter.setPerScreen(AppSettings.contactsPerScreen(this))
        recyclerView = findViewById(R.id.rv_list)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(recyclerView)

        // Only shown when no contacts are saved yet; otherwise setup opens with a long press on a photo.
        // Either way the setup PIN is asked first.
        val btnSetup = findViewById<Button>(R.id.btnSetup)
        btnSetup.setOnClickListener { openSetup() }

        mContactViewModel = ViewModelProvider(this)[ContactViewModel::class.java]
        mContactViewModel.cards.observe(this) { cards ->
            // People with a missed call come first (latest first) until she calls them back.
            // sortedByDescending is stable, so everyone else keeps their usual order.
            val ordered = cards.sortedByDescending { it.missedCallAt ?: 0L }
            adapter.setData(ordered)
            // A missed call is on the first screen, so make sure that's the screen she sees.
            if (ordered.firstOrNull()?.missedCallAt != null) recyclerView.scrollToPosition(0)
            btnSetup.isVisible = cards.isEmpty()
        }

        onBackPressedDispatcher.addCallback(this, blockBackAsHome)

        // Clean up photos of contacts removed last time (kept until now so Undo could restore them).
        if (savedInstanceState == null) mContactViewModel.deleteUnusedPhotos()

        askForMissingPermissions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing Home while already here goes back to the first person.
        recyclerView.scrollToPosition(0)
    }

    override fun onStop() {
        super.onStop()
        // The call screen is in front now: drop the feedback, and accept taps again on return.
        handler.removeCallbacks(endCallingFeedback)
        endCallingFeedback.run()
    }

    override fun onResume() {
        super.onResume()
        blockBackAsHome.isEnabled = HomeScreen.isDefault(this)
        // The grid size may have been changed in setup.
        if (adapter.setPerScreen(AppSettings.contactsPerScreen(this))) recyclerView.scrollToPosition(0)
        // Picks up missed calls, and photos or numbers changed in the Contacts app, while we were away.
        mContactViewModel.refresh()
    }

    private fun askForMissingPermissions() {
        val missing = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissions.launch(missing.toTypedArray())
    }

    /** Returns true if the call was started. */
    private fun call(card: ContactCard): Boolean {
        if (SystemClock.elapsedRealtime() < ignoreTapsUntil) return false

        val number = card.number
        if (number == null) {
            // Happens only for old entries that couldn't be read yet, i.e. without contacts permission.
            askForMissingPermissions()
            return false
        }

        val tel = Uri.fromParts("tel", number, null)
        val canCall = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        try {
            // Without CALL_PHONE, open the dialer with the number filled in instead of crashing.
            startActivity(Intent(if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL, tel))
        } catch (e: SecurityException) {
            startActivity(Intent(Intent.ACTION_DIAL, tel))
        } catch (e: ActivityNotFoundException) {
            // No phone app; nothing more we can do.
            return false
        }

        buzz()
        ignoreTapsUntil = SystemClock.elapsedRealtime() + CALL_START_TIMEOUT_MS
        // Safety net in case the call screen never covers us.
        handler.removeCallbacks(endCallingFeedback)
        handler.postDelayed(endCallingFeedback, CALL_START_TIMEOUT_MS)
        return true
    }

    /** A short buzz, so she feels the tap worked even before anything changes on screen. */
    private fun buzz() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(BUZZ_MS, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(BUZZ_MS)
        }
    }

    private fun openSetup() {
        // Not while a call is starting: a press that lasted a bit too long shouldn't open setup.
        if (SystemClock.elapsedRealtime() < ignoreTapsUntil) return
        SetupLock.requestAccess(this) {
            startActivity(Intent(this, AddContactActivity::class.java))
        }
    }

    private companion object {
        const val CALL_START_TIMEOUT_MS = 6000L
        const val BUZZ_MS = 60L

        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
        )
    }
}
