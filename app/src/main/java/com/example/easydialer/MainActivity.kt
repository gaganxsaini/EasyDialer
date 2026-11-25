package com.example.easydialer

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(recyclerView)

        // Only shown when no contacts are saved yet; otherwise setup opens with a long press on a photo.
        // Either way the setup PIN is asked first.
        val btnSetup = findViewById<Button>(R.id.btnSetup)
        btnSetup.setOnClickListener { openSetup() }

        mContactViewModel = ViewModelProvider(this)[ContactViewModel::class.java]
        mContactViewModel.cards.observe(this) { cards ->
            adapter.setData(cards)
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

    override fun onResume() {
        super.onResume()
        blockBackAsHome.isEnabled = HomeScreen.isDefault(this)
        // The grid size may have been changed in setup.
        if (adapter.setPerScreen(AppSettings.contactsPerScreen(this))) recyclerView.scrollToPosition(0)
        // Picks up photos or numbers changed in the Contacts app while we were away.
        mContactViewModel.refresh()
    }

    private fun askForMissingPermissions() {
        val missing = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissions.launch(missing.toTypedArray())
    }

    private fun call(card: ContactCard) {
        val number = card.number
        if (number == null) {
            // Happens only for old entries that couldn't be read yet, i.e. without contacts permission.
            askForMissingPermissions()
            return
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
        }
    }

    private fun openSetup() {
        SetupLock.requestAccess(this) {
            startActivity(Intent(this, AddContactActivity::class.java))
        }
    }

    private companion object {
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
        )
    }
}
