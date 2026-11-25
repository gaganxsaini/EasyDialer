package com.example.easydialer.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ContactDao {
    @Query("SELECT * FROM contact_table ORDER BY id")
    fun getAll(): LiveData<List<Contact>>

    @Query("SELECT * FROM contact_table ORDER BY id")
    suspend fun getAllOnce(): List<Contact>

    @Insert
    suspend fun insert(contact: Contact)

    @Update
    suspend fun update(contact: Contact)

    @Delete
    suspend fun delete(contact: Contact)
}
