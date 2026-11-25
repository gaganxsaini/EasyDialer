package com.example.easydialer.data

/** Background colours for contacts without a photo; each contact keeps the same colour. */
object ContactColors {
    private val COLORS = intArrayOf(
        0xFF1E88E5.toInt(), // blue
        0xFF43A047.toInt(), // green
        0xFFE53935.toInt(), // red
        0xFFFB8C00.toInt(), // orange
        0xFF8E24AA.toInt(), // purple
        0xFF00897B.toInt(), // teal
    )

    fun forContact(contact: Contact): Int = COLORS[contact.id.mod(COLORS.size)]
}
