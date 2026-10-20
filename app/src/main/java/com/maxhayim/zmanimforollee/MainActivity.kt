package com.maxhayim.zmanimforollee

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var watchText: TextView
    private lateinit var lastMessageText: TextView
    private lateinit var startStopButton: Button

    private var service: ZmanimForegroundService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = (binder as ZmanimForegroundService.LocalBinder).service()
            service?.onStatusChanged = ::refresh
            bound = true
            refresh()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        watchText = findViewById(R.id.watchText)
        lastMessageText = findViewById(R.id.lastMessageText)
        startStopButton = findViewById(R.id.startStopButton)

        startStopButton.setOnClickListener {
            if (isServiceRunning()) stopZmanimService() else startZmanimService()
        }

        requestPermissionsIfNeeded()
        if (isServiceRunning()) bindToService()
        refresh()
    }

    override fun onStart() {
        super.onStart()
        if (isServiceRunning()) bindToService()
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            service?.onStatusChanged = null
            unbindService(connection)
            bound = false
        }
    }

    private fun startZmanimService() {
        startForegroundService(Intent(this, ZmanimForegroundService::class.java))
        bindToService()
        refresh()
    }

    private fun stopZmanimService() {
        if (bound) {
            service?.onStatusChanged = null
            unbindService(connection)
            bound = false
        }
        stopService(Intent(this, ZmanimForegroundService::class.java))
        service = null
        refresh()
    }

    private fun bindToService() {
        bindService(Intent(this, ZmanimForegroundService::class.java), connection, 0)
    }

    private fun isServiceRunning(): Boolean {
        @Suppress("DEPRECATION")
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == ZmanimForegroundService::class.java.name }
    }

    private fun refresh() {
        val running = isServiceRunning()
        startStopButton.text = getString(if (running) R.string.stop_service else R.string.start_service)
        statusText.text = getString(if (running) R.string.status_running else R.string.status_stopped)

        val s = service
        if (s == null) {
            watchText.text = getString(R.string.watch_not_paired)
            lastMessageText.text = ""
            return
        }
        watchText.text = when (s.connectionState) {
            BleConnectionState.READY -> "Watch connected"
            BleConnectionState.CONNECTED -> "Watch connected, Forge service not found"
            BleConnectionState.CONNECTING -> "Connecting to watch…"
            BleConnectionState.DISCONNECTED -> getString(R.string.watch_not_paired)
        }
        val lastMessage = s.lastMessage
        lastMessageText.text = if (lastMessage != null) {
            "${getString(R.string.label_last_message)}: ${lastMessage.drop(1)}"
        } else {
            ""
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += android.Manifest.permission.BLUETOOTH_CONNECT
            permissions += android.Manifest.permission.BLUETOOTH_SCAN
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += android.Manifest.permission.POST_NOTIFICATIONS
        }
        val missing = permissions.filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }
}
