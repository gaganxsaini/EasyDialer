package com.example.easydialer

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.text.InputFilter
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged

/**
 * A 4-digit PIN in front of the setup screen, so an accidental long-press on the dial screen
 * only shows a number pad. It is a child lock, not security, so it is kept in plain prefs.
 */
object SetupLock {
    private const val PREFS = "settings"
    private const val KEY_PIN = "setup_pin"
    private const val PIN_LENGTH = 4

    /** Asks for the PIN (or to create one, the first time) and runs [onUnlocked] when it's right. */
    fun requestAccess(activity: Activity, onUnlocked: () -> Unit) {
        val pin = prefs(activity).getString(KEY_PIN, null)
        if (pin == null) askNewPin(activity, onUnlocked) else askPin(activity, pin, onUnlocked)
    }

    fun changePin(activity: Activity) {
        askNewPin(activity) {
            Toast.makeText(activity, R.string.pin_changed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun askPin(activity: Activity, pin: String, onUnlocked: () -> Unit) {
        val field = pinField(activity, R.string.pin_hint)
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.pin_enter_title)
            .setView(column(activity, field))
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        // Checks as soon as 4 digits are typed; no OK button to find.
        field.doAfterTextChanged { text ->
            if (text?.length == PIN_LENGTH) {
                if (text.toString() == pin) {
                    dialog.dismiss()
                    onUnlocked()
                } else {
                    field.text.clear()
                    field.error = activity.getString(R.string.pin_wrong)
                }
            }
        }
        show(dialog, field)
    }

    private fun askNewPin(activity: Activity, onSaved: () -> Unit) {
        val first = pinField(activity, R.string.pin_new_hint)
        val second = pinField(activity, R.string.pin_repeat_hint)
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.pin_create_title)
            .setMessage(R.string.pin_create_message)
            .setView(column(activity, first, second))
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            // Set here rather than in the builder so a mismatch doesn't close the dialog.
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val pin = first.text.toString()
                when {
                    pin.length != PIN_LENGTH -> first.error = activity.getString(R.string.pin_too_short)
                    pin != second.text.toString() -> second.error = activity.getString(R.string.pin_mismatch)
                    else -> {
                        prefs(activity).edit().putString(KEY_PIN, pin).apply()
                        dialog.dismiss()
                        onSaved()
                    }
                }
            }
        }
        show(dialog, first)
    }

    private fun pinField(context: Context, hintRes: Int) = EditText(context).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        filters = arrayOf(InputFilter.LengthFilter(PIN_LENGTH))
        setHint(hintRes)
    }

    private fun column(context: Context, vararg fields: EditText) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val pad = (24 * context.resources.displayMetrics.density).toInt()
        setPadding(pad, pad / 2, pad, 0)
        fields.forEach { addView(it) }
    }

    private fun show(dialog: AlertDialog, focus: EditText) {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()
        focus.requestFocus()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
