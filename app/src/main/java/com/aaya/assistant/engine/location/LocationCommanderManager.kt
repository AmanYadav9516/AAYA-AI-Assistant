package com.aaya.assistant.engine.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.LocationManager

class LocationCommanderManager(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun setOfficeGeofence(
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = 200f,
        recipientPhone: String,
        messageText: String = "Pahuch gaya"
    ) {
        val intent = Intent(context, GeofenceReceiver::class.java).apply {
            action = GeofenceReceiver.ACTION_GEOFENCE_TRIGGER
            putExtra(GeofenceReceiver.EXTRA_PHONE, recipientPhone)
            putExtra(GeofenceReceiver.EXTRA_MESSAGE, messageText)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            GEOFENCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // Add native Android proximity alert (works without Google Play Services)
        try {
            locationManager.addProximityAlert(
                latitude,
                longitude,
                radiusMeters,
                -1, // No expiration
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeOfficeGeofence() {
        val intent = Intent(context, GeofenceReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            GEOFENCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_MUTABLE
        )
        if (pendingIntent != null) {
            try {
                locationManager.removeProximityAlert(pendingIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val GEOFENCE_REQUEST_CODE = 3001
    }
}
