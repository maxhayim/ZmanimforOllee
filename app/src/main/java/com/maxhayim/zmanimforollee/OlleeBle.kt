package com.maxhayim.zmanimforollee

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import java.util.UUID

/**
 * The Forge BLE GATT service, as documented by the official Ollee Watch Forge SDK
 * (github.com/olleewatch/forge_emulator, examples/bleEcho.js) - not reverse-engineered.
 */
object OlleeBleConstants {
    val SERVICE_UUID: UUID = UUID.fromString("c6fb0001-a37d-40ac-89be-887d25e1f5b3")
    val RX_CHARACTERISTIC_UUID: UUID = UUID.fromString("c6fb0002-a37d-40ac-89be-887d25e1f5b3")
    val TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("c6fb0003-a37d-40ac-89be-887d25e1f5b3")
}

/** Connection state as seen by [OlleeBleClient]. */
enum class BleConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED, // GATT connected, service not yet discovered/ready to write
    READY,     // RX characteristic found, writes will be attempted
}

/**
 * Finds the paired Ollee watch and keeps a BLE GATT connection to it, writing short text
 * messages to the Forge RX characteristic. Pairing itself happens in Android's Bluetooth
 * settings, same as FreeOllee/Super FreeOllee - this class only looks at already-bonded
 * devices, it doesn't do discovery/pairing.
 */
class OlleeBleClient(private val context: Context) {

    interface Listener {
        fun onStateChanged(state: BleConnectionState)
    }

    var listener: Listener? = null

    @Volatile
    var state: BleConnectionState = BleConnectionState.DISCONNECTED
        private set(value) {
            field = value
            listener?.onStateChanged(value)
        }

    private var gatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    /** Bonded device whose name contains "ollee" (case-insensitive), or null if not paired. */
    @SuppressLint("MissingPermission")
    fun findPairedWatch(): BluetoothDevice? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter = manager?.adapter ?: return null
        if (!adapter.isEnabled) return null
        return adapter.bondedDevices?.firstOrNull { it.name?.contains("ollee", ignoreCase = true) == true }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        if (state == BleConnectionState.CONNECTING || state == BleConnectionState.READY) return
        state = BleConnectionState.CONNECTING
        gatt = device.connectGatt(context, true, callback)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        rxCharacteristic = null
        state = BleConnectionState.DISCONNECTED
    }

    /**
     * Writes [message] to RX. Returns true if the write was *started*; delivery isn't
     * confirmed (Forge's RX characteristic doesn't ack at the GATT layer).
     */
    @SuppressLint("MissingPermission")
    fun send(message: String): Boolean {
        val g = gatt ?: return false
        val characteristic = rxCharacteristic ?: return false
        val bytes = message.toByteArray(Charsets.UTF_8)
        if (bytes.size > 20) {
            Log.w(TAG, "message > 20 bytes, Forge RX writes aren't fragmented: $message")
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeCharacteristic(
                characteristic,
                bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            ) == android.bluetooth.BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = bytes
            @Suppress("DEPRECATION")
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            g.writeCharacteristic(characteristic)
        }
    }

    private val callback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    state = BleConnectionState.CONNECTED
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    rxCharacteristic = null
                    state = BleConnectionState.DISCONNECTED
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val service = g.getService(OlleeBleConstants.SERVICE_UUID)
            rxCharacteristic = service?.getCharacteristic(OlleeBleConstants.RX_CHARACTERISTIC_UUID)
            state = if (rxCharacteristic != null) {
                BleConnectionState.READY
            } else {
                Log.w(TAG, "Forge BLE service not found on this device - is it running a Forge build?")
                BleConnectionState.CONNECTED
            }
        }
    }

    companion object {
        private const val TAG = "OlleeBleClient"
    }
}
