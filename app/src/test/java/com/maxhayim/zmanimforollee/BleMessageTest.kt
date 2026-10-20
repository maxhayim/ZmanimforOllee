package com.maxhayim.zmanimforollee

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.OffsetDateTime

class BleMessageTest {

    @Test
    fun `morning time formats with A suffix and no leading zero hour`() {
        val time = OffsetDateTime.parse("2026-10-23T06:53:00-04:00")
        assertEquals("6:53A", BleMessage.timeText(time))
    }

    @Test
    fun `afternoon time formats with P suffix`() {
        val time = OffsetDateTime.parse("2026-10-23T18:08:00-04:00")
        assertEquals("6:08P", BleMessage.timeText(time))
    }

    @Test
    fun `noon and midnight both render as 12`() {
        assertEquals("12:00P", BleMessage.timeText(OffsetDateTime.parse("2026-10-23T12:00:00-04:00")))
        assertEquals("12:00A", BleMessage.timeText(OffsetDateTime.parse("2026-10-23T00:00:00-04:00")))
    }

    @Test
    fun `urgent flag is prefixed as 1, routine as 0`() {
        val zman = Zman("Candle lighting", OffsetDateTime.parse("2026-10-23T18:08:00-04:00"))
        assertEquals("1" + "6:08P", BleMessage.forZman(zman, urgent = true))
        assertEquals("0" + "6:08P", BleMessage.forZman(zman, urgent = false))
    }
}
