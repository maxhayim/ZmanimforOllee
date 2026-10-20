package com.maxhayim.zmanimforollee

import android.content.Context
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** SharedPreferences wrapper. Small enough not to need a DataStore dependency. */
class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("zmanim_for_ollee", Context.MODE_PRIVATE)

    /** Manual lat/lon override; null means "use the device's last known location". */
    var manualLatitude: Double?
        get() = prefs.getString(KEY_LAT, null)?.toDoubleOrNull()
        set(value) = prefs.edit().putString(KEY_LAT, value?.toString()).apply()

    var manualLongitude: Double?
        get() = prefs.getString(KEY_LON, null)?.toDoubleOrNull()
        set(value) = prefs.edit().putString(KEY_LON, value?.toString()).apply()

    var candleLightingMinutes: Int
        get() = prefs.getInt(KEY_CANDLE_MINUTES, 18)
        set(value) = prefs.edit().putInt(KEY_CANDLE_MINUTES, value).apply()

    /** Set while inside a Shabbat/Yom Tov window; polling/BLE is paused until this time. */
    var frozenUntil: OffsetDateTime?
        get() = prefs.getString(KEY_FROZEN_UNTIL, null)?.let { parse(it) }
        set(value) = prefs.edit().putString(KEY_FROZEN_UNTIL, value?.format(ISO)).apply()

    /** The last message actually written to the watch, for dedupe and for the status UI. */
    var lastSentMessage: String?
        get() = prefs.getString(KEY_LAST_MESSAGE, null)
        set(value) = prefs.edit().putString(KEY_LAST_MESSAGE, value).apply()

    var lastSentLabel: String?
        get() = prefs.getString(KEY_LAST_LABEL, null)
        set(value) = prefs.edit().putString(KEY_LAST_LABEL, value).apply()

    private fun parse(raw: String): OffsetDateTime? = try {
        OffsetDateTime.parse(raw, ISO)
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val KEY_LAT = "manual_latitude"
        private const val KEY_LON = "manual_longitude"
        private const val KEY_CANDLE_MINUTES = "candle_lighting_minutes"
        private const val KEY_FROZEN_UNTIL = "frozen_until"
        private const val KEY_LAST_MESSAGE = "last_sent_message"
        private const val KEY_LAST_LABEL = "last_sent_label"
        private val ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    }
}
