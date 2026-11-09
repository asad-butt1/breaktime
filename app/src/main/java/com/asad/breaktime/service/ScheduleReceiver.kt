package com.asad.breaktime.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires when a scheduled break should start. */
class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BreakScheduler.syncSchedules(context)
    }
}
