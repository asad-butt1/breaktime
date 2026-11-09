package com.asad.breaktime

import com.asad.breaktime.data.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleTest {
    private val zone = ZoneId.of("UTC")
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0) =
        ZonedDateTime.of(y, mo, d, h, mi, 0, 0, zone).toInstant().toEpochMilli()

    // 2026-11-09 is a Monday.
    private val evening = Schedule(1, 21 * 60, 23 * 60, Schedule.EVERY_DAY)
    private val overnightWeekdays = Schedule(2, 22 * 60, 7 * 60, Schedule.WEEKDAYS)

    @Test fun inside_window() {
        val w = evening.windowAt(at(2026, 11, 9, 22), zone)
        assertNotNull(w)
        assertEquals(at(2026, 11, 9, 23), w!!.end)
    }

    @Test fun outside_window() = assertNull(evening.windowAt(at(2026, 11, 9, 23, 30), zone))

    @Test fun next_window_tomorrow() =
        assertEquals(at(2026, 11, 10, 21), evening.nextWindow(at(2026, 11, 9, 23, 30), zone)!!.start)

    @Test fun overnight_from_previous_day() {
        // Friday 22:00 → Saturday 07:00 counts, Saturday 03:00 is inside it.
        val w = overnightWeekdays.windowAt(at(2026, 11, 14, 3), zone)
        assertNotNull(w)
        assertEquals(at(2026, 11, 14, 7), w!!.end)
    }

    @Test fun overnight_skips_weekend_start() {
        // Saturday 23:00: no window starts on Saturday.
        assertNull(overnightWeekdays.windowAt(at(2026, 11, 14, 23), zone))
        assertEquals(at(2026, 11, 16, 22), overnightWeekdays.nextWindow(at(2026, 11, 14, 23), zone)!!.start)
    }

    @Test fun disabled_never_runs() = assertNull(evening.copy(enabled = false).windowAt(at(2026, 11, 9, 22), zone))

    @Test fun json_round_trip() {
        val list = listOf(evening, overnightWeekdays.copy(enabled = false))
        assertEquals(list, Schedule.listFromJson(Schedule.listToJson(list)))
    }
}
