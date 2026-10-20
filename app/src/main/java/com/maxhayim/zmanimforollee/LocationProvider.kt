package com.maxhayim.zmanimforollee

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager

data class LatLon(val latitude: Double, val longitude: Double)

/**
 * Location for Hebcal queries: a manual override from settings if set, otherwise the
 * platform LocationManager's last known fix. Uses LocationManager directly (not Play
 * Services' Fused Location Provider) the same way Super FreeOllee does, so it still works
 * on phones without Google Play Services.
 */
class LocationProvider(private val context: Context, private val prefs: Prefs) {

    fun current(): LatLon? {
        prefs.manualLatitude?.let { lat ->
            prefs.manualLongitude?.let { lon ->
                return LatLon(lat, lon)
            }
        }
        return lastKnownFromSystem()
    }

    private fun lastKnownFromSystem(): LatLon? {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            if (!manager.isProviderEnabled(provider)) continue
            val location = try {
                manager.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                null
            } ?: continue
            return LatLon(location.latitude, location.longitude)
        }
        return null
    }
}
