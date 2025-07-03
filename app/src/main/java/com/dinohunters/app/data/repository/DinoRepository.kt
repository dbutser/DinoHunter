// Путь: app/src/main/java/com/dinohunters/app/data/repository/DinoRepository.kt

package com.dinohunters.app.data.repository

import com.dinohunters.app.data.local.BoneDao
import com.dinohunters.app.data.local.BoneZoneDao
import com.dinohunters.app.data.local.UserProfileDao
import com.dinohunters.app.data.model.*
import com.dinohunters.app.service.GeocodingApiService
import com.dinohunters.app.utils.DistanceUtil  // <-- Убедись, что этот импорт есть
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
    private val geocodingApi: GeocodingApiService
) {
    // API ключ (пока оставляем здесь)
    private val googleApiKey = "AIzaSyB3YgLtPfHnkaMFCL4Cj_dTMh9-KGwo81Q"

    // --- НАШИ НОВЫЕ НАСТРОЙКИ ГЕНЕРАЦИИ ---
    // Это радиус каждого круга в метрах. Если он задан в BoneZone.kt, используй то же значение.
    private val ZONE_RADIUS_METERS = 100.0

    // Минимальное расстояние между центрами кругов.
    // Формула: (Радиус * 2) + Запас. Например, 50*2 + 50 = 150м.
    private val MINIMUM_DISTANCE_METERS = 210.0
    // ---

    // --- Все функции до initializeBoneZones остаются без изменений ---
    fun getAllBones(): Flow<List<Bone>> = boneDao.getAllBones()
    fun getAllZones(): Flow<List<BoneZone>> = boneZoneDao.getAllZones()
    fun getUserProfile(): Flow<UserProfile?> = userProfileDao.getProfile()
    suspend fun boneFound(bone: Bone, zoneId: String) {
        addBone(bone)
        markZoneCollected(zoneId)
    }
    private suspend fun addBone(bone: Bone) {
        boneDao.insertBone(bone)
        updateProfileStats()
    }
    private suspend fun markZoneCollected(zoneId: String) {
        boneZoneDao.markZoneCollected(zoneId, System.currentTimeMillis())
    }
    // ---

    // --- ПОЛНОСТЬЮ ОБНОВЛЕННАЯ ЛОГИКА ГЕНЕРАЦИИ ЗОН ---
    suspend fun initializeBoneZones(latitude: Double, longitude: Double) = withContext(Dispatchers.IO) {
        if (boneZoneDao.getAllZones().first().isNotEmpty()) return@withContext

        val validZones = mutableListOf<BoneZone>()
        val numberOfZonesToCreate = 10
        var attempts = 0
        // Увеличим число попыток, т.к. найти свободное место теперь сложнее
        val maxAttempts = 250

        while (validZones.size < numberOfZonesToCreate && attempts < maxAttempts) {
            attempts++
            val (candidateLat, candidateLng) = generateRandomPoint(latitude, longitude)

            // --- Проверка на пересечение ---
            val isOverlapping = validZones.any { existingZone ->
                val distance = DistanceUtil.calculateDistance(
                    candidateLat, candidateLng,
                    existingZone.centerLat, existingZone.centerLng
                )
                distance < MINIMUM_DISTANCE_METERS
            }
            if (isOverlapping) {
                continue // Если пересекается, пропускаем и генерируем новую точку
            }
            // --- Конец проверки ---

            // Проверяем, что точка не в воде и не в парке
            val response = try {
                geocodingApi.reverseGeocode("$candidateLat,$candidateLng", googleApiKey)
            } catch (e: Exception) {
                continue // Ошибка сети, пропускаем
            }

            if (isValidLocation(response)) {
                // Все проверки пройдены! Добавляем зону в список.
                validZones.add(createBoneZone("zone${validZones.size + 1}", candidateLat, candidateLng))
            }
        }

        // Сохраняем все сгенерированные зоны в базу данных
        if (validZones.isNotEmpty()) {
            boneZoneDao.insertAll(validZones)
        }
    }

    private fun generateRandomPoint(latitude: Double, longitude: Double): Pair<Double, Double> {
        val radiusKm = 1.0 // генерируем в радиусе 1 км от игрока
        val latDegreeInKm = 1.0 / 111.0
        val lngDegreeInKm = 1.0 / (111.0 * cos(Math.toRadians(latitude)))

        val angle = Math.random() * 2 * Math.PI
        val distance = Math.random() * radiusKm

        val latOffset = distance * sin(angle) * latDegreeInKm
        val lngOffset = distance * cos(angle) * lngDegreeInKm

        return Pair(latitude + latOffset, longitude + lngOffset)
    }

    private fun isValidLocation(response: GeocodingResponse): Boolean {
        if (response.status != "OK" || response.results.isEmpty()) {
            return false
        }
        val firstResult = response.results.first()
        val forbiddenTypes = listOf("natural_feature", "park", "water", "airport")
        return firstResult.types.none { it in forbiddenTypes }
    }

    // ВАЖНО: Мы добавляем радиус в создаваемый объект!
    private fun createBoneZone(id: String, centerLat: Double, centerLng: Double): BoneZone {
        val maxOffset = 0.0009
        val hiddenLat = centerLat + (Math.random() - 0.5) * maxOffset * 2
        val hiddenLng = centerLng + (Math.random() - 0.5) * maxOffset * 2
        return BoneZone(
            id = id,
            centerLat = centerLat,
            centerLng = centerLng,
            hiddenPointLat = hiddenLat,
            hiddenPointLng = hiddenLng,
            radius = ZONE_RADIUS_METERS // <-- Добавляем радиус
        )
    }

    private suspend fun updateProfileStats() {
        // ... (эта функция остается без изменений)
        val profile = userProfileDao.getProfile().first() ?: UserProfile()
        val totalBones = boneDao.getBonesCount()
        // ... и так далее
        val updatedProfile = profile.copy(
            // ...
            lastActiveAt = System.currentTimeMillis()
        )
        userProfileDao.updateProfile(updatedProfile)
    }
}