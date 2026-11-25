package com.example.easydialer.data

import android.content.Context
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.telephony.PhoneNumberUtils
import androidx.lifecycle.LiveData
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

enum class AddResult { ADDED, DUPLICATE, FAILED }

class ContactRepository(
    private val database: ContactDatabase,
    context: Context,
) {
    private val contactDao = database.contactDao()
    private val resolver = context.contentResolver

    /** Custom photos live here, in the app's private storage, so they survive gallery clean-ups. */
    private val photoDir = File(context.filesDir, "photos")

    val readAllData: LiveData<List<Contact>> = contactDao.getAll()

    /** Saves the phone number the picker returned, unless that number is already on the dial screen. */
    suspend fun addFromPicker(uri: Uri): AddResult = withContext(Dispatchers.IO) {
        val picked = readPhoneRow(uri) ?: return@withContext AddResult.FAILED
        val alreadyAdded = contactDao.getAllOnce().any { sameNumber(it.number, picked.number) }
        if (alreadyAdded) return@withContext AddResult.DUPLICATE
        // New people go to the end of the list.
        contactDao.insert(picked.copy(position = contactDao.maxPosition() + 1))
        AddResult.ADDED
    }

    suspend fun removeContact(contact: Contact) = contactDao.delete(contact)

    suspend fun restoreContact(contact: Contact) = contactDao.insert(contact)

    /** Saves the order from setup: [ids] from first to last. */
    suspend fun saveOrder(ids: List<Int>) = database.withTransaction {
        ids.forEachIndexed { index, id -> contactDao.updatePosition(id, index) }
    }

    /** Copies the image at [source] into the app's storage and uses it as the contact's photo. */
    suspend fun setCustomPhoto(contactId: Int, source: Uri): Boolean = withContext(Dispatchers.IO) {
        val contact = contactDao.getById(contactId) ?: return@withContext false
        photoDir.mkdirs()
        // A new name each time, so Glide doesn't show a cached copy of the previous photo.
        val file = File(photoDir, "${contactId}_${System.currentTimeMillis()}.jpg")
        val copied = try {
            resolver.openInputStream(source)?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            } != null
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        }
        if (!copied || file.length() == 0L) {
            file.delete()
            return@withContext false
        }
        contactDao.updateCustomPhoto(contactId, file.absolutePath)
        contact.customPhoto?.let { File(it).delete() }
        true
    }

    suspend fun removeCustomPhoto(contactId: Int) = withContext(Dispatchers.IO) {
        val contact = contactDao.getById(contactId) ?: return@withContext
        contactDao.updateCustomPhoto(contactId, null)
        contact.customPhoto?.let { File(it).delete() }
    }

    /**
     * Deletes photos of contacts that were removed. Not done at removal time, because Undo
     * brings the contact back with the same photo.
     */
    suspend fun deleteUnusedPhotos() = withContext(Dispatchers.IO) {
        val inUse = contactDao.getAllOnce().mapNotNull { it.customPhoto }.toSet()
        photoDir.listFiles()?.filter { it.absolutePath !in inUse }?.forEach { it.delete() }
    }

    /** Reads each contact's current name, photo and missed-call state. Runs off the main thread. */
    suspend fun resolve(contacts: List<Contact>): List<ContactCard> = withContext(Dispatchers.IO) {
        val recentCalls = readRecentCalls()
        contacts.map { contact ->
            val card = resolveOne(contact)
            card.copy(missedCallAt = missedCallAt(card.number, recentCalls))
        }
    }

    private class LoggedCall(val number: String, val type: Int, val date: Long)

    /** Calls from the last [MISSED_CALL_WINDOW_MS], newest first. Empty without call-log permission. */
    private fun readRecentCalls(): List<LoggedCall> = safeQuery {
        val since = System.currentTimeMillis() - MISSED_CALL_WINDOW_MS
        resolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            "${CallLog.Calls.DATE} > ?",
            arrayOf(since.toString()),
            "${CallLog.Calls.DATE} DESC"
        )?.use { c ->
            val calls = mutableListOf<LoggedCall>()
            while (c.moveToNext()) {
                val number = c.getString(0) ?: continue
                calls += LoggedCall(number, c.getInt(1), c.getLong(2))
            }
            calls
        }
    } ?: emptyList()

    /**
     * The time of the missed call if the latest call with [number] was missed. Any later call with
     * them (she called back, or answered when they rang again), from any app, clears it; so does
     * the call becoming older than the window.
     */
    private fun missedCallAt(number: String?, recentCalls: List<LoggedCall>): Long? {
        if (number == null) return null
        val latest = recentCalls.firstOrNull { sameNumber(it.number, number) } ?: return null
        return if (latest.type == CallLog.Calls.MISSED_TYPE) latest.date else null
    }

    private suspend fun resolveOne(saved: Contact): ContactCard {
        var contact = saved

        // Rows saved before version 2 only have the picker URI: fill in lookup key, number and name once.
        val legacyUri = contact.uri
        if (contact.lookupKey == null && legacyUri != null) {
            val fresh = readPhoneRow(Uri.parse(legacyUri))
                ?.copy(id = contact.id, customPhoto = contact.customPhoto)
            // Every write makes Room re-emit the list, so only write when something changed.
            if (fresh != null && fresh != contact) {
                contactDao.updateDetails(fresh.id, fresh.lookupKey, fresh.number, fresh.name)
                contact = fresh
            }
        }

        // A custom photo wins; skip it if its file has gone missing.
        val customPhoto = contact.customPhoto?.takeIf { File(it).exists() }

        val live = contact.lookupKey?.let { readContact(it) }
            ?: return ContactCard(contact, contact.name, contact.number, customPhoto)

        // Keep the saved number in step with the Contacts app: if it was edited and the
        // contact now has exactly one number, use that one.
        val numbers = readNumbers(live.contactId)
        val number = when {
            numbers.any { sameNumber(it, contact.number) } -> contact.number
            numbers.size == 1 -> numbers[0]
            else -> contact.number
        }

        val updated = contact.copy(lookupKey = live.lookupKey ?: contact.lookupKey, number = number, name = live.name ?: contact.name)
        if (updated != contact) {
            contactDao.updateDetails(updated.id, updated.lookupKey, updated.number, updated.name)
        }

        return ContactCard(updated, live.name ?: updated.name, number, customPhoto ?: live.photoUri)
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

    private companion object {
        /** A missed call stops being marked after this long, even if she never called back. */
        const val MISSED_CALL_WINDOW_MS = 2L * 24 * 60 * 60 * 1000
    }
}
