// Путь к файлу: app/src/main/java/com/dinohunters/app/utils/DistanceUtil.kt

package com.dinohunters.app.utils

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * [НОВЫЙ] Класс-обертка для координат "квадрата", который мы используем для запросов к БД.
 */
data class BoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
)

/**
 * [ИЗМЕНЕНО] Утилита для гео-вычислений. Преобразована из object в class для внедрения зависимостей.
 */
@Singleton // Говорим Hilt, что должен быть только один экземпляр этого класса
class DistanceUtil @Inject constructor() {

    private companion object {
        // Радиус Земли в километрах, так как расчеты для bounding box удобнее вести в км.
        private const val EARTH_RADIUS_KM = 6371.0
    }

    /**
     * Вычисляет расстояние между двумя точками (широта/долгота) в метрах.
     * Использует высокоточную формулу Гаверсинуса.
     * (Ваш код остался без изменений, он корректен)
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

        return (EARTH_RADIUS_KM * c) * 1000 // Умножаем на 1000, чтобы получить метры
    }

    /**
     * [НОВЫЙ] Рассчитывает "квадрат" (bounding box) вокруг центральной точки на заданном расстоянии.
     * Это нужно для быстрой и эффективной выборки зон из базы данных.
     *
     * @param latitude Широта центральной точки.
     * @param longitude Долгота центральной точки.
     * @param distanceMeters Расстояние от центра до граней квадрата в метрах.
     * @return Объект BoundingBox с минимальными и максимальными координатами.
     */
    fun calculateBoundingBox(latitude: Double, longitude: Double, distanceMeters: Double): BoundingBox {
        val distanceKm = distanceMeters / 1000.0

        // Проверяем, чтобы не делить на ноль на полюсах
        if (latitude > 90.0 || latitude < -90.0 || longitude > 180.0 || longitude < -180.0) {
            throw IllegalArgumentException("Invalid coordinates provided.")
        }

        val latRadian = Math.toRadians(latitude)

        // Рассчитываем смещение в градусах для широты и долготы
        val degLat = distanceKm / EARTH_RADIUS_KM * (180.0 / PI)
        val degLon = distanceKm / (EARTH_RADIUS_KM * cos(latRadian)) * (180.0 / PI)

        return BoundingBox(
            minLat = latitude - degLat,
            maxLat = latitude + degLat,
            minLon = longitude - degLon,
            maxLon = longitude + degLon
        )
    }
}