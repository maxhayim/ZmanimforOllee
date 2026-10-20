package com.maxhayim.zmanimforollee

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Owns the whole loop: find the paired watch, hold a BLE connection to it, poll Hebcal on an
 * interval, and push the next zman (or freeze through a Shabbat/Yom Tov window - see
 * docs/design-notes.md's "Shabbat / Yom Tov mode"). Runs as a foreground service so the BLE
 * connection and polling survive the app being backgrounded.
 */
class ZmanimForegroundService : Service() {

    private lateinit var prefs: Prefs
    private lateinit var hebcal: HebcalClient
    private lateinit var locationProvider: LocationProvider
    private lateinit var bleClient: OlleeBleClient
    private lateinit var pollThread: HandlerThread
    private lateinit var pollHandler: Handler

    var onStatusChanged: (() -> Unit)? = null

    val connectionState: BleConnectionState get() = bleClient.state
    val lastMessage: String? get() = prefs.lastSentMessage

    inner class LocalBinder : Binder() {
        fun service(): ZmanimForegroundService = this@ZmanimForegroundService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        hebcal = HebcalClient()
        locationProvider = LocationProvider(this, prefs)
        bleClient = OlleeBleClient(this)
        bleClient.listener = object : OlleeBleClient.Listener {
            override fun onStateChanged(state: BleConnectionState) {
                updateNotification()
                onStatusChanged?.invoke()
                if (state == BleConnectionState.DISCONNECTED) {
                    pollHandler.postDelayed({ connectToPairedWatchIfAny() }, RECONNECT_DELAY_MS)
                }
            }
        }

        pollThread = HandlerThread("ZmanimPoll").apply { start() }
        pollHandler = Handler(pollThread.looper)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        connectToPairedWatchIfAny()
        pollHandler.post(::poll)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        pollHandler.removeCallbacksAndMessages(null)
        pollThread.quitSafely()
        bleClient.disconnect()
        super.onDestroy()
    }

    private fun connectToPairedWatchIfAny() {
        val device = bleClient.findPairedWatch() ?: return
        bleClient.connect(device)
    }

    /** The actual poll/decide/send cycle. Reschedules itself - see class doc. */
    private fun poll() {
        val now = OffsetDateTime.now()
        val frozenUntil = prefs.frozenUntil

        if (frozenUntil != null) {
            if (now.isBefore(frozenUntil)) {
                scheduleNextPoll(frozenUntil)
                return
            }
            // Havdalah has arrived: this is the one message we send without a fresh Hebcal
            // fetch, since we already know the time from when we entered the window.
            send(Zman("Havdalah", frozenUntil, isShabbatBoundary = true), urgent = true)
            prefs.frozenUntil = null
            scheduleNextPoll(now.plusSeconds(POLL_INTERVAL_SECONDS))
            return
        }

        val location = locationProvider.current()
        if (location == null) {
            Log.w(TAG, "no location available (no manual override, no last-known fix) - skipping this poll")
            scheduleNextPoll(now.plusSeconds(POLL_INTERVAL_SECONDS))
            return
        }

        val tzid = ZoneId.systemDefault().id
        val today = LocalDate.now()
        val daily = hebcal.fetchZmanim(location.latitude, location.longitude, tzid, today)
        val windows = hebcal.fetchShabbatWindows(
            location.latitude, location.longitude, tzid,
            prefs.candleLightingMinutes,
            today.minusDays(1), today.plusDays(4),
        )
        if (daily == null || windows == null) {
            Log.w(TAG, "Hebcal fetch failed - skipping this poll, will retry next interval")
            scheduleNextPoll(now.plusSeconds(POLL_INTERVAL_SECONDS))
            return
        }

        val data = ZmanimData(daily, windows)
        val window = ZmanSchedule.currentWindow(now, windows)
        if (window != null) {
            // Just crossed into (or restarted mid-) a Shabbat/Yom Tov window: send one urgent
            // message showing when it ends, then freeze - no more polling or BLE until then.
            send(Zman("Havdalah", window.havdalah, isShabbatBoundary = true), urgent = true)
            prefs.frozenUntil = window.havdalah
            scheduleNextPoll(window.havdalah)
            return
        }

        val next = ZmanSchedule.nextUpcoming(now, data)
        if (next != null && next.label + next.time != prefs.lastSentLabel) {
            send(next, urgent = false)
        }
        scheduleNextPoll(now.plusSeconds(POLL_INTERVAL_SECONDS))
    }

    private fun scheduleNextPoll(notAfter: OffsetDateTime) {
        val now = OffsetDateTime.now()
        val untilTarget = java.time.Duration.between(now, notAfter).toMillis()
        val delay = untilTarget.coerceIn(MIN_POLL_DELAY_MS, POLL_INTERVAL_SECONDS * 1000)
        pollHandler.postDelayed(::poll, delay)
    }

    private fun send(zman: Zman, urgent: Boolean) {
        val message = BleMessage.forZman(zman, urgent)
        val sent = bleClient.send(message)
        if (sent) {
            prefs.lastSentMessage = message
            prefs.lastSentLabel = zman.label + zman.time
        } else {
            Log.w(TAG, "BLE write failed (not connected?) for message: $message")
        }
        updateNotification()
        onStatusChanged?.invoke()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val text = when (bleClient.state) {
            BleConnectionState.READY -> prefs.lastSentMessage?.let { "Sent: ${it.drop(1)}" }
                ?: getString(R.string.notification_text_connected)
            else -> getString(R.string.notification_text_searching)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "ZmanimService"
        private const val CHANNEL_ID = "zmanim_updates"
        private const val NOTIFICATION_ID = 1
        private const val POLL_INTERVAL_SECONDS = 300L // 5 minutes
        private const val MIN_POLL_DELAY_MS = 1_000L
        private const val RECONNECT_DELAY_MS = 10_000L
    }
}
