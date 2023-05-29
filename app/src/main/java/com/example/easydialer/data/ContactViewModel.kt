package com.example.easydialer.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContactViewModel(application: Application) : AndroidViewModel(application) {
    val readAllData : LiveData<List<Contact>>
    private val repository : ContactRepository

    init {
        val contactDao = ContactDatabase.getDatabase(application).contactDao()
        repository = ContactRepository(contactDao)
        readAllData = repository.readAllData
    }

    fun addContact(contact : Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addContact(contact)
        }
    }

    fun removeContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeContact(contact)
        }
    }
}