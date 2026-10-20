package com.maxhayim.zmanimforollee

import java.time.OffsetDateTime

/**
 * Builds the text this app writes to the watch's Forge RX characteristic. See
 * watchface/zmanim.js's header comment for the protocol this must match:
 * byte 0 is '1' (urgent) or '0' (routine), the rest is display text.
 *
 * The watch is a 7-segment LCD, not pixels (docs/design-notes.md's "Watch display" section) -
 * favor digits over letters, so this always sends a time, never a zman name. Ollee.drawMain
 * right-pads/truncates to 6 characters on its own, so there's no need to pad here.
 */
object BleMessage {

    fun forZman(zman: Zman, urgent: Boolean): String {
        val flag = if (urgent) '1' else '0'
        return "$flag${timeText(zman.time)}"
    }

    /** "6:53P", "12:08P" - always 4-6 characters, 12-hour, no leading zero on the hour. */
    fun timeText(time: OffsetDateTime): String {
        val hour24 = time.hour
        val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
        val minute = time.minute
        val suffix = if (hour24 < 12) 'A' else 'P'
        return "$hour12:${minute.toString().padStart(2, '0')}$suffix"
    }
}
