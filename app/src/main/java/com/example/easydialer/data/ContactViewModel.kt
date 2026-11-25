package com.example.easydialer.data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ContactViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContactRepository(
        ContactDatabase.getDatabase(application),
        application
    )

    val readAllData: LiveData<List<Contact>> = repository.readAllData

    private val _cards = MediatorLiveData<List<ContactCard>>()

    /** Saved contacts with their current name, number and photo. */
    val cards: LiveData<List<ContactCard>> = _cards

    private var resolveJob: Job? = null

    init {
        _cards.addSource(readAllData) { resolveCards(it) }
    }

    /** Re-reads names and photos, e.g. after a permission is granted or a contact is edited. */
    fun refresh() {
        readAllData.value?.let { resolveCards(it) }
    }

    private fun resolveCards(contacts: List<Contact>) {
        resolveJob?.cancel()
        resolveJob = viewModelScope.launch {
            _cards.value = repository.resolve(contacts)
        }
    }

    suspend fun addFromPicker(uri: Uri): AddResult = repository.addFromPicker(uri)

    suspend fun setCustomPhoto(contactId: Int, source: Uri): Boolean =
        repository.setCustomPhoto(contactId, source)

    fun removeCustomPhoto(contactId: Int) = viewModelScope.launch {
        repository.removeCustomPhoto(contactId)
    }

    fun deleteUnusedPhotos() = viewModelScope.launch {
        repository.deleteUnusedPhotos()
    }

    fun saveOrder(ids: List<Int>) = viewModelScope.launch {
        repository.saveOrder(ids)
    }

    fun removeContact(contact: Contact) = viewModelScope.launch {
        repository.removeContact(contact)
    }

    fun restoreContact(contact: Contact) = viewModelScope.launch {
        repository.restoreContact(contact)
    }
}
