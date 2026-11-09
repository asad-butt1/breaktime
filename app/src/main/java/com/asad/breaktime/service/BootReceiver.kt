package com.asad.breaktime.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms are cleared when the phone restarts, so set them up again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            BreakScheduler.restore(context)
        }
    }
}
