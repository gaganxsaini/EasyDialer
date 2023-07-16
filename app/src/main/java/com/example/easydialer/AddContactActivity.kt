package com.example.easydialer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.easydialer.data.Contact
import com.example.easydialer.data.ContactViewModel
import com.example.easydialer.data.ViewContactListAdapter

class AddContactActivity : AppCompatActivity() {
    private lateinit var btnSave : Button
    private lateinit var mContactViewModel : ContactViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contact)
        btnSave = findViewById(R.id.btnSave)

        mContactViewModel = ViewModelProvider(this).get(ContactViewModel::class.java)

        btnSave.setOnClickListener{
            //insertDataToDatabase()
            launchContactPicker()
        }

        val adapter = ViewContactListAdapter()
        val recyclerView = findViewById<RecyclerView>(R.id.rv_list_all)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        PagerSnapHelper().attachToRecyclerView(recyclerView)

        mContactViewModel = ViewModelProvider(this).get(ContactViewModel::class.java)
        mContactViewModel.readAllData.observe(this){
            adapter.differ.submitList(it)
        }

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos  = viewHolder.adapterPosition
                val contact = adapter.differ.currentList[pos]
                mContactViewModel.removeContact(contact)
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).apply {
            attachToRecyclerView(recyclerView)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_CONTACT && resultCode == RESULT_OK && data != null) {
            val contactUri : Uri? = data.data
            if (contactUri != null)
                insertDataToDatabase(contactUri)
        }
    }

    private fun insertDataToDatabase(uri : Uri){
        val contact = Contact(0, uri.toString())
        mContactViewModel.addContact(contact)
        Toast.makeText(this, "Successfully added", Toast.LENGTH_LONG).show()
    }

    private val REQUEST_CODE_PICK_CONTACT = 1

    private fun launchContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT)
    }
}