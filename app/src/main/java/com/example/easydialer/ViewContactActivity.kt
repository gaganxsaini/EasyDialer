package com.example.easydialer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.easydialer.data.ContactViewModel
import com.example.easydialer.data.ViewContactListAdapter

class ViewContactActivity : ComponentActivity() {
    private lateinit var mContactViewModel: ContactViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_contacts)

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

}


