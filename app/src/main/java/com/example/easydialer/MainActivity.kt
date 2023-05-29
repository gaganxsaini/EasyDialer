package com.example.easydialer

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import com.example.easydialer.data.ContactViewModel
import com.example.easydialer.data.ListAdapter


class MainActivity : AppCompatActivity() {
    private lateinit var mContactViewModel: ContactViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val adapter = ListAdapter()
        val recyclerView = findViewById<RecyclerView>(R.id.rv_list)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

        PagerSnapHelper().attachToRecyclerView(recyclerView)

        mContactViewModel = ViewModelProvider(this).get(ContactViewModel::class.java)
        mContactViewModel.readAllData.observe(this, Observer {contact ->
            adapter.setData(contact)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item : MenuItem) : Boolean {
        when (item.itemId) {
            R.id.menu_action_add ->
                startActivity(Intent (this, AddContactActivity::class.java));
            R.id.menu_action_view ->
                startActivity(Intent (this, ViewContactActivity::class.java));
        }
        return true
    }
}