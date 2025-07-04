// Путь: app/src/main/java/com/dinohunters/app/data/repository/DinoRepository.kt

package com.dinohunters.app.data.repository

import android.location.Location
import android.util.Log
import com.dinohunters.app.data.local.BoneDao
import com.dinohunters.app.data.local.BoneZoneDao
import com.dinohunters.app.data.local.UserProfileDao
import com.dinohunters.app.data.model.*
import com.dinohunters.app.service.GeocodingApiService
import com.dinohunters.app.utils.DistanceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin

@Singleton
class DinoRepository @Inject constructor(
    private val boneDao: BoneDao,
    private val boneZoneDao: BoneZoneDao,
    private val userProfileDao: UserProfileDao,
    private val geocodingApi: GeocodingApiService,
    private val distanceUtil: DistanceUtil
) {
    // ЗАМЕНИТЕ НА ВАШ КЛЮЧ!
    private val googleApiKey = "AIzaSyB3YgLtPfHnkaMFCL4Cj_dTMh9-KGwo81Q"

    // --- КОНСТАНТЫ ИГРЫ ---
    private val ZONE_RADIUS_METERS = 100.0
    private val MINIMUM_DISTANCE_BETWEEN_ZONES_METERS = 210.0
    private val PLAYER_AURA_RADIUS_METERS = 1000.0
    private val ZONES_TO_GENERATE_COUNT = 10

    // --- Основные методы (без изменений) ---
    fun getAllBones(): Flow<List<Bone>> = boneDao.getAllBones()
    fun getUserProfile(): Flow<UserProfile?> = userProfileDao.getProfile()

    suspend fun boneFound(bone: Bone, zoneId: String) {
        addBone(bone)
        markZoneAsCollected(zoneId)
    }

    private suspend fun addBone(bone: Bone) {
        boneDao.insertBone(bone)
        updateProfileStats()
    }

    private suspend fun markZoneAsCollected(zoneId: String) {
        boneZoneDao.markZoneCollected(zoneId, System.currentTimeMillis())
    }

    suspend fun getVisibleZones(playerLocation: Location): List<BoneZone> = withContext(Dispatchers.IO) {
        val boundingBox = distanceUtil.calculateBoundingBox(
            playerLocation.latitude,
            playerLocation.longitude,
            PLAYER_AURA_RADIUS_METERS
        )
        val zonesInBox = boneZoneDao.getZonesInBoundingBox(
            minLat = boundingBox.minLat,
            maxLat = boundingBox.maxLat,
            minLon = boundingBox.minLon,
            maxLon = boundingBox.maxLon
        )
        return@withContext zonesInBox.filter { zone ->
            distanceUtil.calculateDistance(
                playerLocation.latitude, playerLocation.longitude,
                zone.centerLat, zone.centerLng
            ) <= PLAYER_AURA_RADIUS_METERS
        }
    }

    suspend fun ensureZonesExist(playerLocation: Location): Boolean = withContext(Dispatchers.IO) {
        val hasNearbyZones = getVisibleZones(playerLocation).isNotEmpty()
        if (!hasNearbyZones) {
            generateNewZonesAround(playerLocation)
            return@withContext true
        }
        return@withContext false
    }

    suspend fun performGreatPurge() = withContext(Dispatchers.IO) {
        boneZoneDao.deleteAllZones()
    }


    // --- [ИЗМЕНЕННЫЙ МЕТОД] ГЕНЕРАЦИЯ ЗОН ---

    private suspend fun generateNewZonesAround(centerLocation: Location) {
        val allExistingZones = boneZoneDao.getAllZonesList()
        val validZonesToSave = mutableListOf<BoneZone>()
        var attempts = 0
        // Увеличено, чтобы дать больше шансов на успех
        val maxAttempts = 500

        while (validZonesToSave.size < ZONES_TO_GENERATE_COUNT && attempts < maxAttempts) {
            attempts++

            // 1. Генерируем случайную точку-кандидата в ауре игрока
            val (candidateLat, candidateLng) = generateRandomPoint(
                centerLocation.latitude,
                centerLocation.longitude,
                PLAYER_AURA_RADIUS_METERS
            )

            // 2. [НОВОЕ] Привязываем кандидата к ближайшей дороге с помощью Roads API
            val finalPoint = try {
                val response = geocodingApi.snapToRoad(
                    path = "$candidateLat,$candidateLng",
                    apiKey = googleApiKey
                )
                // Берем первую точку из ответа. Если ответа нет - null
                response.snappedPoints?.firstOrNull()?.location
            } catch (e: Exception) {
                Log.e("DinoRepository", "Roads API request failed", e)
                null // Ошибка сети, пропускаем эту попытку
            }

            // Если точка не привязалась к дороге (например, в океане), пропускаем
            if (finalPoint == null) {
                continue
            }

            // 3. Проверяем, не пересекается ли НАША НОВАЯ ТОЧКА НА ДОРОГЕ с уже существующими
            val isOverlapping = allExistingZones.any { existingZone ->
                distanceUtil.calculateDistance(
                    finalPoint.latitude, finalPoint.longitude,
                    existingZone.centerLat, existingZone.centerLng
                ) < MINIMUM_DISTANCE_BETWEEN_ZONES_METERS
            } || validZonesToSave.any { newZone ->
                distanceUtil.calculateDistance(
                    finalPoint.latitude, finalPoint.longitude,
                    newZone.centerLat, newZone.centerLng
                ) < MINIMUM_DISTANCE_BETWEEN_ZONES_METERS
            }

            if (isOverlapping) {
                // Эта точка на дороге слишком близко к другой зоне, ищем новую
                continue
            }

            // 4. Все проверки пройдены! Добавляем зону, используя "привязанные" координаты.
            validZonesToSave.add(
                createBoneZone(
                    id = "zone_${System.currentTimeMillis()}_${validZonesToSave.size}",
                    centerLat = finalPoint.latitude,
                    centerLng = finalPoint.longitude
                )
            )
        }
        Log.d("DinoRepository", "Generated ${validZonesToSave.size} new zones.")
        // Сохраняем все новые валидные зоны в базу данных
        if (validZonesToSave.isNotEmpty()) {
            boneZoneDao.insertAll(validZonesToSave)
        }
    }

    // --- Вспомогательные методы (без изменений) ---

    private fun generateRandomPoint(latitude: Double, longitude: Double, radiusMeters: Double): Pair<Double, Double> {
        val radiusInDegrees = radiusMeters / 111320.0
        val u = Math.random()
        val v = Math.random()
        val w = radiusInDegrees * Math.sqrt(u)
        val t = 2 * Math.PI * v
        val x = w * cos(t)
        val y = w * sin(t) / cos(Math.toRadians(latitude))
        return Pair(latitude + y, longitude + x)
    }

    private fun createBoneZone(id: String, centerLat: Double, centerLng: Double): BoneZone {
        val hiddenPointRadiusMeters = ZONE_RADIUS_METERS * 0.8
        val (hiddenLat, hiddenLng) = generateRandomPoint(centerLat, centerLng, hiddenPointRadiusMeters)

        return BoneZone(
            id = id,
            centerLat = centerLat,
            centerLng = centerLng,
            hiddenPointLat = hiddenLat,
            hiddenPointLng = hiddenLng,
            radius = ZONE_RADIUS_METERS,
            isCollected = false,
            collectedAt = 0L
        )
    }

    private suspend fun updateProfileStats() {
        // ...
    }
}