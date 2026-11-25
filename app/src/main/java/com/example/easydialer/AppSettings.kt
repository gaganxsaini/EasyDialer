package com.example.easydialer

import android.content.Context

/** Small settings kept in the app's preferences. */
object AppSettings {
    const val PREFS = "settings"
    private const val KEY_PER_SCREEN = "contacts_per_screen"

    /** How many contacts each screen of the dial screen shows. */
    val PER_SCREEN_CHOICES = intArrayOf(1, 2, 4, 6, 9)

    fun contactsPerScreen(context: Context): Int =
        prefs(context).getInt(KEY_PER_SCREEN, 1).takeIf { it in PER_SCREEN_CHOICES } ?: 1

    fun setContactsPerScreen(context: Context, count: Int) {
        prefs(context).edit().putInt(KEY_PER_SCREEN, count).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
