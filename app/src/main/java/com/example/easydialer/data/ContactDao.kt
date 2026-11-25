package com.example.easydialer.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactDao {
    @Query("SELECT * FROM contact_table ORDER BY position, id")
    fun getAll(): LiveData<List<Contact>>

    @Query("SELECT * FROM contact_table ORDER BY position, id")
    suspend fun getAllOnce(): List<Contact>

    @Insert
    suspend fun insert(contact: Contact)

    @Query("SELECT * FROM contact_table WHERE id = :id")
    suspend fun getById(id: Int): Contact?

    // Targeted updates, so a background refresh of names/numbers can't overwrite a photo set
    // meanwhile (and the other way round).
    @Query("UPDATE contact_table SET lookup_key = :lookupKey, number = :number, name = :name WHERE id = :id")
    suspend fun updateDetails(id: Int, lookupKey: String?, number: String?, name: String?)

    @Query("SELECT COALESCE(MAX(position), 0) FROM contact_table")
    suspend fun maxPosition(): Int

    @Query("UPDATE contact_table SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Int, position: Int)

    @Query("UPDATE contact_table SET custom_photo = :path WHERE id = :id")
    suspend fun updateCustomPhoto(id: Int, path: String?)

    @Delete
    suspend fun delete(contact: Contact)
}
