package com.example.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Automatically invoked by Android OS when a newly downloaded APK update has completed installation.
 * Restarts the application seamlessly.
 */
class PackageReplacedReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "PackageReplacedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "New APK version successfully installed. Restarting Scribe Craft Global...")
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("EXTRA_APP_JUST_UPDATED", true)
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart application after update", e)
            }
        }
    }
}
