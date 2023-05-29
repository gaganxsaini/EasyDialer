package com.example.easydialer.data

import androidx.lifecycle.LiveData

class ContactRepository(private val contactDao : ContactDao) {
    val readAllData : LiveData<List<Contact>> = contactDao.getAll()

    suspend fun addContact(contact: Contact) {
        contactDao.insert(contact)
    }

    suspend fun removeContact(contact: Contact) {
        contactDao.delete(contact)
    }
}