package com.example.easydialer.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.telephony.PhoneNumberUtils
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AddResult { ADDED, DUPLICATE, FAILED }

class ContactRepository(
    private val contactDao: ContactDao,
    private val resolver: ContentResolver,
) {
    val readAllData: LiveData<List<Contact>> = contactDao.getAll()

    /** Saves the phone number the picker returned, unless that number is already on the dial screen. */
    suspend fun addFromPicker(uri: Uri): AddResult = withContext(Dispatchers.IO) {
        val picked = readPhoneRow(uri) ?: return@withContext AddResult.FAILED
        val alreadyAdded = contactDao.getAllOnce().any { sameNumber(it.number, picked.number) }
        if (alreadyAdded) return@withContext AddResult.DUPLICATE
        contactDao.insert(picked)
        AddResult.ADDED
    }

    suspend fun removeContact(contact: Contact) = contactDao.delete(contact)

    suspend fun restoreContact(contact: Contact) = contactDao.insert(contact)

    /** Reads each contact's current name and photo. Runs off the main thread. */
    suspend fun resolve(contacts: List<Contact>): List<ContactCard> = withContext(Dispatchers.IO) {
        contacts.map { resolveOne(it) }
    }

    private suspend fun resolveOne(saved: Contact): ContactCard {
        var contact = saved

        // Rows saved before version 2 only have the picker URI: fill in lookup key, number and name once.
        val legacyUri = contact.uri
        if (contact.lookupKey == null && legacyUri != null) {
            val fresh = readPhoneRow(Uri.parse(legacyUri))?.copy(id = contact.id)
            // Every write makes Room re-emit the list, so only write when something changed.
            if (fresh != null && fresh != contact) {
                contactDao.update(fresh)
                contact = fresh
            }
        }

        val live = contact.lookupKey?.let { readContact(it) }
            ?: return ContactCard(contact, contact.name, contact.number, null)

        // Keep the saved number in step with the Contacts app: if it was edited and the
        // contact now has exactly one number, use that one.
        val numbers = readNumbers(live.contactId)
        val number = when {
            numbers.any { sameNumber(it, contact.number) } -> contact.number
            numbers.size == 1 -> numbers[0]
            else -> contact.number
        }

        val updated = contact.copy(lookupKey = live.lookupKey ?: contact.lookupKey, number = number, name = live.name ?: contact.name)
        if (updated != contact) contactDao.update(updated)

        return ContactCard(updated, live.name ?: updated.name, number, live.photoUri)
    }

    /** Reads the phone-number row returned by the contact picker. */
    private fun readPhoneRow(uri: Uri): Contact? = safeQuery {
        resolver.query(
            uri,
            arrayOf(Contacts.LOOKUP_KEY, Phone.NUMBER, Contacts.DISPLAY_NAME),
            null, null, null
        )?.use { c ->
            if (!c.moveToFirst()) return@use null
            val number = c.getString(1) ?: return@use null
            Contact(uri = uri.toString(), lookupKey = c.getString(0), number = number, name = c.getString(2))
        }
    }

    private class LiveContact(val contactId: Long, val lookupKey: String?, val name: String?, val photoUri: String?)

    /** Finds the contact by lookup key, which still works after the contact is edited or synced. */
    private fun readContact(lookupKey: String): LiveContact? = safeQuery {
        val uri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey)
        resolver.query(
            uri,
            arrayOf(Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME, Contacts.PHOTO_URI),
            null, null, null
        )?.use { c ->
            if (!c.moveToFirst()) return@use null
            LiveContact(c.getLong(0), c.getString(1), c.getString(2), c.getString(3))
        }
    }

    private fun readNumbers(contactId: Long): List<String> = safeQuery {
        resolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.NUMBER),
            "${Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { c ->
            val numbers = mutableListOf<String>()
            while (c.moveToNext()) c.getString(0)?.let { numbers += it }
            numbers.distinctBy { PhoneNumberUtils.normalizeNumber(it) }
        }
    } ?: emptyList()

    private fun sameNumber(a: String?, b: String?): Boolean =
        a != null && b != null && PhoneNumberUtils.compare(a, b)

    /** Contacts can't be read without permission or if a row disappeared; treat that as "not found". */
    private inline fun <T> safeQuery(block: () -> T?): T? =
        try {
            block()
        } catch (e: SecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
}
