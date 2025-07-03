package com.dinohunters.app.data.repository

import com.dinohunters.app.data.local.BoneDao
import com.dinohunters.app.data.local.BoneZoneDao
import com.dinohunters.app.data.local.UserProfileDao
import com.dinohunters.app.data.model.*
import com.dinohunters.app.service.GeocodingApiService
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
    private val geocodingApi: GeocodingApiService // Новая зависимость
) {
    // API ключ нужно безопасно хранить, но для примера возьмем так
    // Вставьте сюда тот же ключ, что и в AndroidManifest.xml
    private val googleApiKey = "AIzaSyB3YgLtPfHnkaMFCL4Cj_dTMh9-KGwo81Q"

    // --- Старые функции остаются без изменений ---
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
    // ... (остальные вспомогательные функции, если они есть, тоже остаются)


    // --- НОВАЯ ЛОГИКА ГЕНЕРАЦИИ ЗОН ---
    suspend fun initializeBoneZones(latitude: Double, longitude: Double) = withContext(Dispatchers.IO) {
        if (boneZoneDao.getAllZones().first().isNotEmpty()) return@withContext

        val validZones = mutableListOf<BoneZone>()
        val numberOfZonesToCreate = 10
        var attempts = 0
        val maxAttempts = 100 // Чтобы не уйти в бесконечный цикл

        while (validZones.size < numberOfZonesToCreate && attempts < maxAttempts) {
            attempts++
            val (candidateLat, candidateLng) = generateRandomPoint(latitude, longitude)
            val response = try {
                geocodingApi.reverseGeocode("$candidateLat,$candidateLng", googleApiKey)            } catch (e: Exception) {
                // Ошибка сети, пропускаем попытку
                continue
            }

            if (isValidLocation(response)) {
                validZones.add(createBoneZone("zone${validZones.size + 1}", candidateLat, candidateLng))
            }
        }

        validZones.forEach { zone -> boneZoneDao.insertZone(zone) }
    }

    private fun generateRandomPoint(latitude: Double, longitude: Double): Pair<Double, Double> {
        val radiusKm = 1.0
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
            return false // Точка в океане или пустыне, Google ничего не нашел
        }
        val firstResult = response.results.first()
        // Проверяем, что это не вода, не парк и не что-то природное
        val forbiddenTypes = listOf("natural_feature", "park", "water", "airport")
        return firstResult.types.none { it in forbiddenTypes }
    }

    // Вспомогательные функции, которые были у нас раньше
    private fun createBoneZone(id: String, centerLat: Double, centerLng: Double): BoneZone {
        val maxOffset = 0.0009
        val hiddenLat = centerLat + (Math.random() - 0.5) * maxOffset * 2
        val hiddenLng = centerLng + (Math.random() - 0.5) * maxOffset * 2
        return BoneZone(
            id = id,
            centerLat = centerLat,
            centerLng = centerLng,
            hiddenPointLat = hiddenLat,
            hiddenPointLng = hiddenLng
        )
    }

    private suspend fun updateProfileStats() {
        val profile = userProfileDao.getProfile().first() ?: UserProfile()
        val totalBones = boneDao.getBonesCount()
        val commonBones = boneDao.getBonesByRarity(BoneRarity.COMMON)
        val uncommonBones = boneDao.getBonesByRarity(BoneRarity.UNCOMMON)
        val rareBones = boneDao.getBonesByRarity(BoneRarity.RARE)
        val epicBones = boneDao.getBonesByRarity(BoneRarity.EPIC)
        val legendaryBones = boneDao.getBonesByRarity(BoneRarity.LEGENDARY)

        val updatedProfile = profile.copy(
            totalBones = totalBones,
            commonBones = commonBones,
            uncommonBones = uncommonBones,
            rareBones = rareBones,
            epicBones = epicBones,
            legendaryBones = legendaryBones,
            lastActiveAt = System.currentTimeMillis()
        )
        userProfileDao.updateProfile(updatedProfile)
    }
}