package com.example.easydialer.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactDao {
    @Query("SELECT * FROM contact_table")
    fun getAll(): LiveData<List<Contact>>

    @Insert
    fun insert(contact: Contact)

    @Delete
    fun delete(contact: Contact)
}