package edu.mit.privatekit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils

/**
 * author: Dmitry Brant, 2020
 */
class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (TextUtils.isEmpty(action)) {
            return
        }
        when (action) {
            Intent.ACTION_BOOT_COMPLETED ->
                // To simulate:
                // `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED`
                startServiceOnBoot(context)
        }
    }

    private fun startServiceOnBoot(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(LocationService.PREFS_NAME, 0)
        val started = prefs.getBoolean(LocationService.PREF_IS_STARTED, false)
        if (started) {
            LocationService.start(context)
        }
    }
}