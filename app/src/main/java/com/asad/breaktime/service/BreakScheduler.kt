package com.asad.breaktime.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.app.Notification
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.asad.breaktime.R
import com.asad.breaktime.data.BreakStore
import com.asad.breaktime.ui.MainActivity

/** Starts and ends breaks, and handles the alarm and notifications around them. */
object BreakScheduler {

    private const val CHANNEL_DONE = "breaks"
    private const val CHANNEL_TIMER = "break_timer"
    private const val NOTIF_ONGOING = 1
    private const val NOTIF_DONE = 2

    fun startBreak(context: Context, minutes: Int) {
        val store = BreakStore.get(context)
        store.startBreak(minutes)
        scheduleEndAlarm(context, store.breakEndsAt)
        showOngoing(context, store.breakEndsAt)
    }

    fun endEarly(context: Context) {
        BreakStore.get(context).endEarly()
        cancelEndAlarm(context)
        NotificationManagerCompat.from(context).cancel(NOTIF_ONGOING)
    }

    /** Call whenever the break might be over. Shows the "apps unlocked" notification once. */
    fun finishIfDue(context: Context) {
        if (BreakStore.get(context).finishIfDue()) {
            NotificationManagerCompat.from(context).cancel(NOTIF_ONGOING)
            notify(
                context, NOTIF_DONE,
                Notification.Builder(context, CHANNEL_DONE)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Break complete. Nice work!")
                    .setContentText("Your apps are unlocked again.")
                    .setContentIntent(openAppIntent(context))
                    .setAutoCancel(true)
                    .build()
            )
        }
    }

    /** After a phone restart, put the alarm back. */
    fun restore(context: Context) {
        val store = BreakStore.get(context)
        if (store.isBreakActive()) {
            scheduleEndAlarm(context, store.breakEndsAt)
            showOngoing(context, store.breakEndsAt)
        } else {
            finishIfDue(context)
        }
    }

    private fun scheduleEndAlarm(context: Context, at: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        // Inexact but allowed without extra permissions. The lock itself checks the clock,
        // so apps unlock on time even if this alarm (which only sends the notification) is a bit late.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, endIntent(context))
    }

    private fun cancelEndAlarm(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(endIntent(context))
    }

    private fun endIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, BreakEndReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun showOngoing(context: Context, endsAt: Long) {
        notify(
            context, NOTIF_ONGOING,
            Notification.Builder(context, CHANNEL_TIMER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("On a break")
                .setContentText("Your apps are locked. Go enjoy some time offline.")
                .setWhen(endsAt)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openAppIntent(context))
                .build()
        )
    }

    private fun notify(context: Context, id: Int, notification: Notification) {
        ensureChannels(context)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMER, "Break timer", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Break finished", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}
