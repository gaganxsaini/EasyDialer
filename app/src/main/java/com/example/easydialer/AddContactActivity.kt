package com.example.easydialer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.easydialer.data.AddResult
import com.example.easydialer.data.ContactCard
import com.example.easydialer.data.ContactViewModel
import com.example.easydialer.data.ViewContactListAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

class AddContactActivity : AppCompatActivity() {
    private lateinit var mContactViewModel: ContactViewModel

    private val pickContact =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            if (result.resultCode == RESULT_OK && uri != null) addPicked(uri)
        }

    // The contact whose photo is being changed, and where the camera should save. Kept in the saved
    // state because the camera app can push this activity out of memory on a low-RAM phone.
    private var photoContactId = NO_CONTACT
    private var cameraUri: Uri? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) savePhoto(uri)
        }

    private val takePhoto =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val uri = cameraUri
            if (saved && uri != null) savePhoto(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contact)

        mContactViewModel = ViewModelProvider(this)[ContactViewModel::class.java]

        savedInstanceState?.let {
            photoContactId = it.getInt(KEY_PHOTO_CONTACT, NO_CONTACT)
            cameraUri = it.getString(KEY_CAMERA_URI)?.let(Uri::parse)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            pickContact.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
        }

        val adapter = ViewContactListAdapter(onClick = ::showPhotoOptions)
        val recyclerView = findViewById<RecyclerView>(R.id.rv_list_all)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        mContactViewModel.cards.observe(this) {
            adapter.submit(it)
        }

        // Press and hold to drag a person to a new place; swipe left or right to remove, with Undo.
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            private var orderChanged = false

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                adapter.move(from, to)
                orderChanged = true
                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) adapter.dragging = true
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // Dropped: save the new order once, rather than on every step of the drag.
                if (orderChanged) {
                    orderChanged = false
                    mContactViewModel.saveOrder(adapter.currentList.map { it.contact.id })
                }
                adapter.dragging = false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                val card = adapter.currentList.getOrNull(pos)
                if (pos == RecyclerView.NO_POSITION || card == null) {
                    adapter.notifyDataSetChanged()
                    return
                }
                mContactViewModel.removeContact(card.contact)
                Snackbar.make(
                    recyclerView,
                    getString(R.string.contact_removed, card.name ?: card.number ?: ""),
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.undo) {
                    mContactViewModel.restoreContact(card.contact)
                }.show()
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PHOTO_CONTACT, photoContactId)
        outState.putString(KEY_CAMERA_URI, cameraUri?.toString())
    }

    private fun showPhotoOptions(card: ContactCard) {
        val hasCustom = card.contact.customPhoto != null
        val options = listOfNotNull(
            getString(R.string.photo_gallery),
            getString(R.string.photo_camera),
            if (hasCustom) getString(R.string.photo_remove) else null,
        )
        AlertDialog.Builder(this)
            .setTitle(card.name ?: card.number)
            .setItems(options.toTypedArray()) { _, which ->
                photoContactId = card.contact.id
                when (which) {
                    0 -> pickImage.launch("image/*")
                    1 -> launchCamera()
                    2 -> mContactViewModel.removeCustomPhoto(card.contact.id)
                }
            }
            .show()
    }

    private fun launchCamera() {
        val dir = File(cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "capture.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        cameraUri = uri
        try {
            takePhoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_camera, Toast.LENGTH_LONG).show()
        }
    }

    private fun savePhoto(source: Uri) {
        val contactId = photoContactId
        if (contactId == NO_CONTACT) return
        lifecycleScope.launch {
            val ok = mContactViewModel.setCustomPhoto(contactId, source)
            Toast.makeText(
                this@AddContactActivity,
                if (ok) R.string.photo_saved else R.string.photo_failed,
                Toast.LENGTH_SHORT
            ).show()
            // The camera's copy has been taken into the app's storage; free the space.
            if (source == cameraUri) File(File(cacheDir, "camera"), "capture.jpg").delete()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.setup_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_per_screen -> {
            choosePerScreen()
            true
        }
        R.id.menu_regular_home -> {
            HomeScreen.openRegular(this)
            true
        }
        R.id.menu_change_home -> {
            HomeScreen.openHomeSettings(this)
            true
        }
        R.id.menu_change_pin -> {
            SetupLock.changePin(this)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun choosePerScreen() {
        val choices = AppSettings.PER_SCREEN_CHOICES
        val labels = choices.map { resources.getQuantityString(R.plurals.per_screen_option, it, it) }
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_per_screen)
            .setSingleChoiceItems(
                labels.toTypedArray(),
                choices.indexOf(AppSettings.contactsPerScreen(this))
            ) { dialog, which ->
                AppSettings.setContactsPerScreen(this, choices[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun addPicked(uri: Uri) {
        lifecycleScope.launch {
            val message = when (mContactViewModel.addFromPicker(uri)) {
                AddResult.ADDED -> R.string.contact_added
                AddResult.DUPLICATE -> R.string.contact_already_added
                AddResult.FAILED -> R.string.contact_add_failed
            }
            Toast.makeText(this@AddContactActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    private companion object {
        const val NO_CONTACT = -1
        const val KEY_PHOTO_CONTACT = "photo_contact_id"
        const val KEY_CAMERA_URI = "camera_uri"
    }
}
