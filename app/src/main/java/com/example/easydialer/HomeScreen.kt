package com.example.easydialer

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast

/** Helpers for running EasyDialer as the phone's home screen, and for getting back out of it. */
object HomeScreen {
    private const val SAMSUNG_LAUNCHER = "com.sec.android.app.launcher"

    // Android's placeholder home screen shown while the phone boots; never a real choice.
    private const val SETTINGS_PACKAGE = "com.android.settings"

    private fun homeIntent() = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

    /** True when EasyDialer is the home screen the Home button opens. */
    fun isDefault(context: Context): Boolean {
        val home = context.packageManager.resolveActivity(homeIntent(), PackageManager.MATCH_DEFAULT_ONLY)
        return home?.activityInfo?.packageName == context.packageName
    }

    /**
     * Opens the phone's regular home screen (Samsung's on her phone). Pressing Home afterwards
     * still comes back to EasyDialer while it is the default.
     */
    fun openRegular(activity: Activity) {
        val homes = activity.packageManager.queryIntentActivities(homeIntent(), 0)
            .map { it.activityInfo }
            .filter { it.packageName != activity.packageName && it.packageName != SETTINGS_PACKAGE }
        val target = homes.firstOrNull { it.packageName == SAMSUNG_LAUNCHER } ?: homes.firstOrNull()

        if (target == null) {
            Toast.makeText(activity, R.string.no_other_home, Toast.LENGTH_LONG).show()
            return
        }
        activity.startActivity(
            homeIntent()
                .setClassName(target.packageName, target.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Opens the system setting that picks the home app, to switch back to the regular one for good. */
    fun openHomeSettings(activity: Activity) {
        // Not every phone has the first two screens, so fall back step by step.
        val actions = listOf(
            Settings.ACTION_HOME_SETTINGS,
            Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
            Settings.ACTION_SETTINGS,
        )
        for (action in actions) {
            try {
                activity.startActivity(Intent(action))
                return
            } catch (e: ActivityNotFoundException) {
                // try the next one
            }
        }
    }
}
