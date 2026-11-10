package com.asad.breaktime.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One break the user took. */
data class BreakRecord(
    val startedAt: Long,
    val plannedMinutes: Int,
    val completed: Boolean,
)

/**
 * Everything the app remembers, saved on the phone.
 * Small enough that plain SharedPreferences is the simplest reliable choice.
 */
class BreakStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("breaktime", Context.MODE_PRIVATE)

    private val _changes = MutableStateFlow(0)

    /** Bumps every time something changes so screens can refresh. */
    val changes: StateFlow<Int> = _changes

    private fun changed() {
        _changes.value = _changes.value + 1
    }

    // ---- First run ----

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()
            changed()
        }

    /** User agreed to the Accessibility disclosure (required by Google Play before opening settings). */
    var accessibilityConsent: Boolean
        get() = prefs.getBoolean(KEY_CONSENT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_CONSENT, value).apply()
            changed()
        }

    /** When the user was last sent to Accessibility settings (0 = not waiting). Lets us bring them back. */
    var awaitingLockSetupAt: Long
        get() = prefs.getLong(KEY_AWAIT_SETUP, 0L)
        set(value) {
            prefs.edit().putLong(KEY_AWAIT_SETUP, value).apply()
        }

    // ---- Premium ----

    /** Cached from Google Play Billing (refreshed every time the app opens). */
    var isPremium: Boolean
        get() = prefs.getBoolean(KEY_PREMIUM, false)
        set(value) {
            if (value == isPremium) return
            prefs.edit().putBoolean(KEY_PREMIUM, value).apply()
            changed()
        }

    /** How many apps the user may lock. */
    val appLimit: Int get() = if (isPremium) Int.MAX_VALUE else FREE_APP_LIMIT

    /** Premium setting: breaks can't be ended early. */
    var strictMode: Boolean
        get() = prefs.getBoolean(KEY_STRICT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_STRICT, value).apply()
            changed()
        }

    /** True when the current break was started in strict mode. */
    val breakIsStrict: Boolean get() = prefs.getBoolean(KEY_BREAK_STRICT, false)

    // ---- Scheduled breaks (Premium) ----

    var schedules: List<Schedule>
        get() = Schedule.listFromJson(prefs.getString(KEY_SCHEDULES, null))
        set(value) {
            prefs.edit().putString(KEY_SCHEDULES, Schedule.listToJson(value)).apply()
            changed()
        }

    /** "scheduleId@windowStart" of the last scheduled break started, so one window starts only once. */
    var lastAutoStart: String?
        get() = prefs.getString(KEY_LAST_AUTO, null)
        set(value) {
            prefs.edit().putString(KEY_LAST_AUTO, value).apply()
        }

    // ---- Locked apps ----

    var lockedApps: Set<String>
        get() = prefs.getStringSet(KEY_LOCKED, emptySet())?.toSet() ?: emptySet()
        set(value) {
            prefs.edit().putStringSet(KEY_LOCKED, value.toSet()).apply()
            changed()
        }

    // ---- Current break ----

    val breakEndsAt: Long get() = prefs.getLong(KEY_END, 0L)
    val breakStartedAt: Long get() = prefs.getLong(KEY_START, 0L)
    val breakMinutes: Int get() = prefs.getInt(KEY_MINUTES, 0)

    fun isBreakActive(now: Long = System.currentTimeMillis()): Boolean {
        val end = breakEndsAt
        return end > 0L && now < end
    }

    fun startBreak(minutes: Int, now: Long = System.currentTimeMillis()) =
        startBreakUntil(now + minutes * 60_000L, now)

    /** Starts a break that ends at [endsAt] (used by scheduled breaks). */
    fun startBreakUntil(endsAt: Long, now: Long = System.currentTimeMillis()) {
        val minutes = ((endsAt - now + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
        prefs.edit()
            .putLong(KEY_START, now)
            .putLong(KEY_END, endsAt)
            .putInt(KEY_MINUTES, minutes)
            .putBoolean(KEY_BREAK_STRICT, isPremium && strictMode)
            .apply()
        changed()
    }

    /**
     * If a break's time is up, records it as completed and clears it.
     * Safe to call any number of times. Returns true only the one time it finishes a break.
     */
    @Synchronized
    fun finishIfDue(now: Long = System.currentTimeMillis()): Boolean {
        val end = breakEndsAt
        if (end == 0L || now < end) return false
        val minutes = breakMinutes
        addRecord(BreakRecord(breakStartedAt, minutes, completed = true))
        prefs.edit().putInt(KEY_CELEBRATE, minutes).apply()
        clearBreak()
        return true
    }

    /** User gave up before the timer ended. */
    @Synchronized
    fun endEarly() {
        if (breakEndsAt == 0L || breakIsStrict) return
        addRecord(BreakRecord(breakStartedAt, breakMinutes, completed = false))
        clearBreak()
    }

    private fun clearBreak() {
        prefs.edit().remove(KEY_END).remove(KEY_START).remove(KEY_MINUTES).remove(KEY_BREAK_STRICT).apply()
        changed()
    }

    /** Minutes of a just-finished break whose "Break complete" screen hasn't been shown yet (0 = none). */
    val pendingCelebration: Int get() = prefs.getInt(KEY_CELEBRATE, 0)

    fun clearCelebration() {
        prefs.edit().remove(KEY_CELEBRATE).apply()
        changed()
    }

    // ---- History ----

    val history: List<BreakRecord>
        get() {
            val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
            return try {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    BreakRecord(o.getLong("s"), o.getInt("m"), o.getBoolean("c"))
                }.sortedByDescending { it.startedAt }
            } catch (e: Exception) {
                emptyList()
            }
        }

    private fun addRecord(record: BreakRecord) {
        val arr = try {
            JSONArray(prefs.getString(KEY_HISTORY, "[]"))
        } catch (e: Exception) {
            JSONArray()
        }
        arr.put(JSONObject().put("s", record.startedAt).put("m", record.plannedMinutes).put("c", record.completed))
        // Keep the file small: only the latest 500 breaks.
        val trimmed = JSONArray()
        val from = maxOf(0, arr.length() - 500)
        for (i in from until arr.length()) trimmed.put(arr.get(i))
        prefs.edit().putString(KEY_HISTORY, trimmed.toString()).apply()
    }

    /** Days in a row (ending today or yesterday) with at least one completed break. */
    fun currentStreak(today: LocalDate = LocalDate.now()): Int {
        val zone = ZoneId.systemDefault()
        val days = history.filter { it.completed }
            .map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
            .toSet()
        var day = if (today in days) today else today.minusDays(1)
        var streak = 0
        while (day in days) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    companion object {
        private const val KEY_LOCKED = "locked_apps"
        private const val KEY_END = "break_end"
        private const val KEY_START = "break_start"
        private const val KEY_MINUTES = "break_minutes"
        private const val KEY_HISTORY = "history"
        private const val KEY_ONBOARDED = "onboarding_done"
        private const val KEY_CONSENT = "accessibility_consent"
        private const val KEY_CELEBRATE = "celebrate_minutes"
        private const val KEY_PREMIUM = "premium"
        private const val KEY_AWAIT_SETUP = "await_setup"
        private const val KEY_SCHEDULES = "schedules"
        private const val KEY_LAST_AUTO = "last_auto_start"
        private const val KEY_STRICT = "strict_mode"
        private const val KEY_BREAK_STRICT = "break_strict"

        /** Free plan limit. Premium removes it. */
        const val FREE_APP_LIMIT = 3

        @Volatile
        private var instance: BreakStore? = null

        fun get(context: Context): BreakStore =
            instance ?: synchronized(this) {
                instance ?: BreakStore(context).also { instance = it }
            }
    }
}
