package com.asad.breaktime.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * A repeating break, e.g. every weekday 21:00 to 23:00.
 * Times are minutes after midnight. If [endMinute] is earlier than [startMinute] the break runs past midnight.
 * [days] uses 1 = Monday … 7 = Sunday and refers to the day the break starts.
 */
data class Schedule(
    val id: Long,
    val startMinute: Int,
    val endMinute: Int,
    val days: Set<Int>,
    val enabled: Boolean = true,
) {
    val durationMinutes: Int get() = ((endMinute - startMinute) + 24 * 60) % (24 * 60)

    /** A specific run of this schedule: [start, end) in epoch millis. */
    data class Window(val schedule: Schedule, val start: Long, val end: Long) {
        val key: String get() = "${schedule.id}@$start"
    }

    private fun windowStartingOn(date: LocalDate, zone: ZoneId): Window? {
        if (date.dayOfWeek.value !in days || durationMinutes == 0) return null
        val start = ZonedDateTime.of(date.atStartOfDay(), zone).plusMinutes(startMinute.toLong())
        val end = start.plusMinutes(durationMinutes.toLong())
        return Window(this, start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli())
    }

    /** The run happening at [now], if any. */
    fun windowAt(now: Long, zone: ZoneId = ZoneId.systemDefault()): Window? {
        if (!enabled) return null
        val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return listOf(today.minusDays(1), today)
            .mapNotNull { windowStartingOn(it, zone) }
            .firstOrNull { now >= it.start && now < it.end }
    }

    /** The next run that starts after [now]. */
    fun nextWindow(now: Long, zone: ZoneId = ZoneId.systemDefault()): Window? {
        if (!enabled) return null
        val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return (0..7L).asSequence()
            .mapNotNull { windowStartingOn(today.plusDays(it), zone) }
            .firstOrNull { it.start > now }
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("s", startMinute)
        .put("e", endMinute)
        .put("d", JSONArray(days.sorted()))
        .put("on", enabled)

    companion object {
        val EVERY_DAY = (1..7).toSet()
        val WEEKDAYS = (1..5).toSet()
        val WEEKENDS = setOf(6, 7)

        fun fromJson(o: JSONObject): Schedule {
            val d = o.getJSONArray("d")
            return Schedule(
                id = o.getLong("id"),
                startMinute = o.getInt("s"),
                endMinute = o.getInt("e"),
                days = (0 until d.length()).map { d.getInt(it) }.toSet(),
                enabled = o.optBoolean("on", true),
            )
        }

        fun listFromJson(raw: String?): List<Schedule> {
            if (raw == null) return emptyList()
            return try {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun listToJson(list: List<Schedule>): String = JSONArray(list.map { it.toJson() }).toString()

        fun dayLetter(day: Int): String = DayOfWeek.of(day).name.take(1)

        fun describeDays(days: Set<Int>): String = when (days) {
            EVERY_DAY -> "Every day"
            WEEKDAYS -> "Weekdays"
            WEEKENDS -> "Weekends"
            else -> days.sorted().joinToString(", ") { DayOfWeek.of(it).name.take(3).lowercase().replaceFirstChar(Char::uppercase) }
        }
    }
}
