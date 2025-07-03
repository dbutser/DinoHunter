// Путь к файлу: app/src/main/java/com/dinohunters/app/utils/DistanceUtil.kt

package com.dinohunters.app.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Утилита для вычисления расстояния между двумя географическими точками.
 */
object DistanceUtil {
    // Радиус Земли в метрах
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Вычисляет расстояние между двумя точками (широта/долгота) в метрах.
     * Использует высокоточную формулу Гаверсинуса.
     *
     * @param lat1 Широта первой точки.
     * @param lon1 Долгота первой точки.
     * @param lat2 Широта второй точки.
     * @param lon2 Долгота второй точки.
     * @return Расстояние в метрах.
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }
}