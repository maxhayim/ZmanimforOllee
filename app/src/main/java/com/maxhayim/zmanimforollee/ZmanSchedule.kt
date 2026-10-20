package com.maxhayim.zmanimforollee

import java.time.OffsetDateTime

/**
 * Pure scheduling logic: given fetched zmanim/windows and the current time, what's next and
 * are we currently inside a Shabbat/Yom Tov window. No network, no BLE, no state - easy to
 * unit test. [ZmanimForegroundService] owns the actual freeze/resume bookkeeping.
 */
object ZmanSchedule {

    /** The window (candle lighting through Havdalah) we're currently inside, if any. */
    fun currentWindow(now: OffsetDateTime, windows: List<ShabbatWindow>): ShabbatWindow? =
        windows.firstOrNull { !now.isBefore(it.candles) && now.isBefore(it.havdalah) }

    /**
     * The earliest upcoming zman after [now], across the day's regular zmanim plus any
     * candle-lighting/Havdalah boundaries in [data]. Call this only when [currentWindow] is
     * null - once we're inside a window, the service freezes instead of scheduling through it.
     */
    fun nextUpcoming(now: OffsetDateTime, data: ZmanimData): Zman? {
        val boundaries = data.windows.flatMap {
            listOf(
                Zman("Candle lighting", it.candles, isShabbatBoundary = true),
                Zman("Havdalah", it.havdalah, isShabbatBoundary = true),
            )
        }
        return (data.dailyZmanim + boundaries)
            .filter { it.time.isAfter(now) }
            .minByOrNull { it.time }
    }
}
