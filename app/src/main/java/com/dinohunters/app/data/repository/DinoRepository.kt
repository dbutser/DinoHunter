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
    private val googleApiKey = "YOUR_GOOGLE_API_KEY_HERE"

    // --- КОНСТАНТЫ ИГРЫ ---
    private val ZONE_RADIUS_METERS = 100.0
    private val MINIMUM_DISTANCE_BETWEEN_ZONES_METERS = 210.0
    private val PLAYER_AURA_RADIUS_METERS = 1000.0
    private val ZONES_TO_GENERATE_COUNT = 10
    private val MAX_GENERATION_ATTEMPTS = 500

    // --- ПУБЛИЧНЫЕ МЕТОДЫ ДЛЯ VIEWMODEL ---

    fun getAllBones(): Flow<List<Bone>> = boneDao.getAllBones()

    fun getUserProfile(): Flow<UserProfile?> = userProfileDao.getProfile()

    suspend fun boneFound(bone: Bone, zoneId: String) {
        addBone(bone)
        markZoneAsCollected(zoneId)
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
        if (getVisibleZones(playerLocation).isEmpty()) {
            generateNewZonesAround(playerLocation)
            return@withContext true
        }
        return@withContext false
    }

    suspend fun performGreatPurge() = withContext(Dispatchers.IO) {
        boneZoneDao.deleteAllZones()
    }

    // --- ПРИВАТНЫЕ МЕТОДЫ РЕПОЗИТОРИЯ ---

    private suspend fun addBone(bone: Bone) {
        boneDao.insertBone(bone)
        updateProfileStats()
    }

    private suspend fun markZoneAsCollected(zoneId: String) {
        boneZoneDao.markZoneCollected(zoneId, System.currentTimeMillis())
    }

    private suspend fun updateProfileStats() = withContext(Dispatchers.IO) {
        val currentProfile = userProfileDao.getProfile().first() ?: UserProfile()

        val total = boneDao.getBonesCount()
        val common = boneDao.getBonesCountByRarity(BoneRarity.COMMON)
        val uncommon = boneDao.getBonesCountByRarity(BoneRarity.UNCOMMON)
        val rare = boneDao.getBonesCountByRarity(BoneRarity.RARE)
        val epic = boneDao.getBonesCountByRarity(BoneRarity.EPIC)
        val legendary = boneDao.getBonesCountByRarity(BoneRarity.LEGENDARY)

        val updatedProfile = currentProfile.copy(
            totalBones = total,
            commonBones = common,
            uncommonBones = uncommon,
            rareBones = rare,
            epicBones = epic,
            legendaryBones = legendary,
            lastActiveAt = System.currentTimeMillis()
        )

        userProfileDao.updateProfile(updatedProfile)
        Log.d("DinoRepository", "Profile stats updated. Total bones: $total")
    }

    private suspend fun generateNewZonesAround(centerLocation: Location) {
        val allExistingZones = boneZoneDao.getAllZonesList()
        val validZonesToSave = mutableListOf<BoneZone>()
        var attempts = 0

        while (validZonesToSave.size < ZONES_TO_GENERATE_COUNT && attempts < MAX_GENERATION_ATTEMPTS) {
            attempts++
            val (candidateLat, candidateLng) = generateRandomPoint(
                centerLocation.latitude,
                centerLocation.longitude,
                PLAYER_AURA_RADIUS_METERS
            )

            val finalPoint = try {
                val response = geocodingApi.snapToRoad(
                    path = "$candidateLat,$candidateLng",
                    apiKey = googleApiKey
                )
                response.snappedPoints?.firstOrNull()?.location
            } catch (e: Exception) {
                Log.e("DinoRepository", "Roads API request failed", e)
                null
            }

            if (finalPoint == null) continue

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

            if (isOverlapping) continue

            validZonesToSave.add(
                createBoneZone(
                    id = "zone_${System.currentTimeMillis()}_${validZonesToSave.size}",
                    centerLat = finalPoint.latitude,
                    centerLng = finalPoint.longitude
                )
            )
        }

        Log.d("DinoRepository", "Generated ${validZonesToSave.size} new zones.")
        if (validZonesToSave.isNotEmpty()) {
            boneZoneDao.insertAll(validZonesToSave)
        }
    }

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
}