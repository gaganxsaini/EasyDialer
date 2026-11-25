package com.example.easydialer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.easydialer.data.AddResult
import com.example.easydialer.data.ContactViewModel
import com.example.easydialer.data.ViewContactListAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AddContactActivity : AppCompatActivity() {
    private lateinit var mContactViewModel: ContactViewModel

    private val pickContact =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            if (result.resultCode == RESULT_OK && uri != null) addPicked(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contact)

        mContactViewModel = ViewModelProvider(this)[ContactViewModel::class.java]

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            pickContact.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
        }

        val adapter = ViewContactListAdapter()
        val recyclerView = findViewById<RecyclerView>(R.id.rv_list_all)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        mContactViewModel.cards.observe(this) {
            adapter.differ.submitList(it)
        }

        // Swipe left or right to remove, with Undo. Reordering isn't supported yet, so no drag.
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                val card = adapter.differ.currentList.getOrNull(pos)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.setup_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
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
}
