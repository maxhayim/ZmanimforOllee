package com.maxhayim.zmanimforollee

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime

class ZmanScheduleTest {

    private fun at(hour: Int, minute: Int = 0) =
        OffsetDateTime.parse("2026-10-23T${"%02d".format(hour)}:${"%02d".format(minute)}:00-04:00")

    @Test
    fun `nextUpcoming picks the earliest time after now, across daily zmanim and windows`() {
        val data = ZmanimData(
            dailyZmanim = listOf(
                Zman("Mincha G", at(13, 0)),
                Zman("Mincha K", at(16, 30)),
            ),
            windows = listOf(ShabbatWindow(candles = at(18, 0), havdalah = at(19, 30))),
        )
        val next = ZmanSchedule.nextUpcoming(at(14, 0), data)
        assertEquals("Mincha K", next?.label)
    }

    @Test
    fun `nextUpcoming returns the candle lighting boundary when it is soonest`() {
        val data = ZmanimData(
            dailyZmanim = listOf(Zman("Mincha K", at(16, 30))),
            windows = listOf(ShabbatWindow(candles = at(18, 0), havdalah = at(19, 30))),
        )
        val next = ZmanSchedule.nextUpcoming(at(17, 0), data)
        assertEquals("Candle lighting", next?.label)
        assertEquals(at(18, 0), next?.time)
    }

    @Test
    fun `nextUpcoming returns null once everything today has passed`() {
        val data = ZmanimData(
            dailyZmanim = listOf(Zman("Sunset", at(18, 0))),
            windows = emptyList(),
        )
        assertNull(ZmanSchedule.nextUpcoming(at(23, 0), data))
    }

    @Test
    fun `currentWindow is null before candle lighting and after havdalah`() {
        val windows = listOf(ShabbatWindow(candles = at(18, 0), havdalah = at(19, 30)))
        assertNull(ZmanSchedule.currentWindow(at(17, 59), windows))
        assertNull(ZmanSchedule.currentWindow(at(19, 30), windows))
    }

    @Test
    fun `currentWindow is non-null from candle lighting up to (not including) havdalah`() {
        val windows = listOf(ShabbatWindow(candles = at(18, 0), havdalah = at(19, 30)))
        assertEquals(windows[0], ZmanSchedule.currentWindow(at(18, 0), windows))
        assertEquals(windows[0], ZmanSchedule.currentWindow(at(19, 29), windows))
    }
}
