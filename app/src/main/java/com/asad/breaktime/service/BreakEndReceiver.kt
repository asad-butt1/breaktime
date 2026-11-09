package com.asad.breaktime.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires when the break timer runs out. */
class BreakEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BreakScheduler.finishIfDue(context)
    }
}
