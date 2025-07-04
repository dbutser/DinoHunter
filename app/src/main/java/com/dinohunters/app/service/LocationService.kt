// Путь: app/src/main/java/com/dinohunters/app/service/LocationService.kt

package com.dinohunters.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Возвращает Flow с постоянными обновлениями местоположения.
     * (Ваш код остался без изменений, он корректен)
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(interval: Long): Flow<Location> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d("LocationService", "New location update: $location")
                    trySend(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        awaitClose {
            Log.d("LocationService", "Stopping location updates.")
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * [НОВЫЙ МЕТОД]
     * Асинхронно и однократно получает последнее известное местоположение.
     * Это очень быстрый вызов, который не тратит батарею.
     * Возвращает null, если местоположение неизвестно (например, на новом устройстве).
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? {
        // Мы используем корутину, чтобы превратить старый callback-based API в современный suspend-метод
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (continuation.isActive) {
                        Log.d("LocationService", "Last known location received: $location")
                        continuation.resume(location)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        Log.e("LocationService", "Failed to get last known location", exception)
                        continuation.resumeWithException(exception)
                    }
                }
                .addOnCanceledListener {
                    if (continuation.isActive) {
                        continuation.cancel()
                    }
                }
        }
    }
}