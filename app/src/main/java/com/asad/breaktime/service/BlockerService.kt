package com.asad.breaktime.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.asad.breaktime.data.BreakStore
import com.asad.breaktime.ui.BlockActivity

/**
 * Notices when a locked app comes to the front during a break and covers it
 * with the break screen. Only the app's package name is used; screen content is never read.
 */
class BlockerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        BreakScheduler.finishIfDue(this)
        val store = BreakStore.get(this)
        // Alarms can be a few minutes late on a sleeping phone, so also catch a scheduled break here.
        if (!store.isBreakActive() && pkg in store.lockedApps) BreakScheduler.syncSchedules(this)
        if (store.isBreakActive() && pkg in store.lockedApps) {
            startActivity(
                Intent(this, BlockActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(BlockActivity.EXTRA_PACKAGE, pkg)
            )
        }
    }

    override fun onInterrupt() = Unit

    companion object {
        /** Whether the user has switched this on in Accessibility settings. */
        fun isEnabled(context: Context): Boolean {
            val me = ComponentName(context, BlockerService::class.java)
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
