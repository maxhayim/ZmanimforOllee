package com.maxhayim.zmanimforollee

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Talks to the public Hebcal REST APIs (hebcal.com). No API key, no scraping.
 *
 * Important: both endpoints need `latitude`/`longitude` (not `lat`/`lon`) and a `tzid` -
 * confirmed against the live API, since Hebcal's own docs page doesn't spell this out.
 */
class HebcalClient {

    /** Daily halachic times for [date] at [lat]/[lon]. Null on any network/parse failure. */
    fun fetchZmanim(lat: Double, lon: Double, tzid: String, date: LocalDate): List<Zman>? {
        val url = "https://www.hebcal.com/zmanim?cfg=json" +
            "&latitude=$lat&longitude=$lon&tzid=${encode(tzid)}&date=$date"
        val json = get(url) ?: return null
        val times = json.optJSONObject("times") ?: return null
        return DAILY_ZMANIM.mapNotNull { (key, label) ->
            val raw = times.optString(key, "")
            if (raw.isEmpty()) return@mapNotNull null
            parseTime(raw)?.let { Zman(label, it) }
        }
    }

    /**
     * Candle lighting / Havdalah windows between [start] (inclusive) and [end] (exclusive).
     * Pass a few days on either side of "today" so a window that started yesterday, or starts
     * tomorrow, is still visible to the scheduler.
     */
    fun fetchShabbatWindows(
        lat: Double,
        lon: Double,
        tzid: String,
        candleLightingMinutes: Int,
        start: LocalDate,
        end: LocalDate,
    ): List<ShabbatWindow>? {
        val url = "https://www.hebcal.com/hebcal?cfg=json" +
            "&latitude=$lat&longitude=$lon&tzid=${encode(tzid)}" +
            "&c=on&b=$candleLightingMinutes&M=on" +
            "&maj=off&min=off&mod=off&nx=off&ss=off&mf=off&d=off&o=off" +
            "&start=$start&end=$end"
        val json = get(url) ?: return null
        val items = json.optJSONArray("items") ?: return null

        val candles = mutableListOf<OffsetDateTime>()
        val havdalahs = mutableListOf<OffsetDateTime>()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val time = parseTime(item.optString("date", "")) ?: continue
            when (item.optString("category")) {
                "candles" -> candles.add(time)
                "havdalah" -> havdalahs.add(time)
            }
        }
        candles.sort()
        havdalahs.sort()

        // Pair each candle lighting with the next havdalah strictly after it. Handles back
        // to back Yom Tov / Shabbat chains the same way, since each pairing only looks forward.
        return candles.mapNotNull { candleTime ->
            val havdalahTime = havdalahs.firstOrNull { it.isAfter(candleTime) } ?: return@mapNotNull null
            ShabbatWindow(candleTime, havdalahTime)
        }
    }

    private fun get(url: String): JSONObject? {
        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty(
                    "User-Agent",
                    "ZmanimForOllee/0.1 (+https://github.com/maxhayim/ZmanimforOllee)"
                )
            }
            connection.inputStream.use { stream ->
                val body = BufferedReader(InputStreamReader(stream)).readText()
                JSONObject(body)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Hebcal request failed: $url", e)
            null
        }
    }

    private fun BufferedReader.readText(): String {
        val sb = StringBuilder()
        var line: String?
        while (readLine().also { line = it } != null) sb.append(line)
        return sb.toString()
    }

    private fun parseTime(raw: String): OffsetDateTime? = try {
        OffsetDateTime.parse(raw)
    } catch (e: Exception) {
        null
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        private const val TAG = "HebcalClient"

        // GRA-based times by default; see design-notes.md's open question about supporting
        // MGA/Baal HaTanya as a configurable opinion later.
        private val DAILY_ZMANIM = listOf(
            "alotHaShachar" to "Alot",
            "misheyakir" to "Misheyakir",
            "sunrise" to "Sunrise",
            "sofZmanShma" to "Shema",
            "sofZmanTfilla" to "Tfilla",
            "chatzot" to "Chatzot",
            "minchaGedola" to "Mincha G",
            "minchaKetana" to "Mincha K",
            "plagHaMincha" to "Plag",
            "sunset" to "Sunset",
            "tzeit72min" to "Tzeit",
        )
    }
}
