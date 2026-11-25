package com.example.easydialer.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A contact chosen for the dial screen.
 *
 * [uri] is the phone-number row the picker returned. Android can change that row's id when a
 * contact is edited or synced, so the lookup key and number are stored too: the lookup key
 * survives edits, and the number lets a call go through even if the contact can't be read.
 */
@Entity(tableName = "contact_table")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "uri") val uri: String?,
    @ColumnInfo(name = "lookup_key") val lookupKey: String? = null,
    @ColumnInfo(name = "number") val number: String? = null,
    @ColumnInfo(name = "name") val name: String? = null,
    /** Absolute path of a photo chosen in setup, kept in the app's own storage. Shown instead of the contact photo. */
    @ColumnInfo(name = "custom_photo") val customPhoto: String? = null,
)

/** A saved contact with its current name and photo (custom photo first, else the contact's own). */
data class ContactCard(
    val contact: Contact,
    val name: String?,
    val number: String?,
    val photoUri: String?,
)
