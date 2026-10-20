package com.maxhayim.zmanimforollee

import java.time.OffsetDateTime

/** One halachic time, as returned by Hebcal. */
data class Zman(
    val label: String,
    val time: OffsetDateTime,
    /** True for candle lighting / Havdalah - the transition messages get the urgent chime. */
    val isShabbatBoundary: Boolean = false,
)

/** A single Shabbat/Yom Tov window: candle lighting through Havdalah. */
data class ShabbatWindow(
    val candles: OffsetDateTime,
    val havdalah: OffsetDateTime,
)

/** Everything fetched from Hebcal for a date range, parsed and ready to schedule from. */
data class ZmanimData(
    val dailyZmanim: List<Zman>,
    val windows: List<ShabbatWindow>,
)
