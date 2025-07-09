package com.dinohunters.app.data.repository

import android.location.Location
import android.util.Log
import com.dinohunters.app.data.local.BoneDao
import com.dinohunters.app.data.local.BoneZoneDao
import com.dinohunters.app.data.local.UserProfileDao
import com.dinohunters.app.data.model.*
import com.dinohunters.app.service.GeocodingApiService
import com.dinohunters.app.service.StepCounter
import com.dinohunters.app.utils.DistanceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
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
    private val distanceUtil: DistanceUtil,
    private val stepCounter: StepCounter
) {
    // ЗАМЕНИТЕ НА ВАШ КЛЮЧ!
    private val googleApiKey = "AIzaSyB3YgLtPfHnkaMFCL4Cj_dTMh9-KGwo81Q"

    // --- КОНСТАНТЫ ИГРЫ ---
    private val ZONE_RADIUS_METERS = 100.0
    private val MINIMUM_DISTANCE_BETWEEN_ZONES_METERS = 210.0
    private val PLAYER_AURA_RADIUS_METERS = 1500.0
    private val ZONES_TO_GENERATE_COUNT = 15
    private val MAX_GENERATION_ATTEMPTS = 500

    // --- ПУБЛИЧНЫЕ МЕТОДЫ ДЛЯ VIEWMODEL ---

    // --- ЛОГИКА ДИНОКОИНОВ ---
    suspend fun syncDinoCoins() = withContext(Dispatchers.IO) {
        Log.d("DinoRepository", "--- Запущена синхронизация динокоинов ---")

        val profile = userProfileDao.getProfile().firstOrNull()
        if (profile == null) {
            Log.e("DinoRepository", "Ошибка: Профиль пользователя не найден в базе данных.")
            return@withContext
        }
        Log.d("DinoRepository", "Профиль загружен. Последние шаги в базе: ${profile.lastSyncedSteps}, Баланс: ${profile.dinocoinBalance}")

        val totalStepsFromSensor = stepCounter.getTotalSteps().firstOrNull()
        if (totalStepsFromSensor == null) {
            Log.e("DinoRepository", "Ошибка: Не удалось получить данные с датчика шагов (вернулся null). Проверьте разрешения и наличие датчика.")
            return@withContext
        }
        Log.d("DinoRepository", "Данные с датчика получены. Всего шагов с перезагрузки: $totalStepsFromSensor")

        // Вычисляем только НОВЫЕ шаги
        val newSteps = totalStepsFromSensor - profile.lastSyncedSteps
        Log.d("DinoRepository", "Вычислены новые шаги: $newSteps (Сенсор: $totalStepsFromSensor - База: ${profile.lastSyncedSteps})")

        if (newSteps > 0) {
            val newBalance = profile.dinocoinBalance + newSteps
            // Обновляем и баланс, и общее количество "учтенных" шагов
            userProfileDao.updateDinoCoinsAndSteps(newBalance, totalStepsFromSensor)
            Log.d("DinoRepository", "УСПЕХ! Начислено $newSteps динокоинов. Новый баланс: $newBalance. Новое значение шагов в базе: $totalStepsFromSensor")
        } else {
            Log.w("DinoRepository", "Новых шагов для начисления нет (newSteps <= 0). Обновление не требуется.")
        }
        Log.d("DinoRepository", "--- Синхронизация завершена ---")
    }

    suspend fun spendDinoCoins(amount: Long) = withContext(Dispatchers.IO) {
        val profile = userProfileDao.getProfile().firstOrNull() ?: return@withContext
        if (profile.dinocoinBalance >= amount) {
            val newBalance = profile.dinocoinBalance - amount
            userProfileDao.updateDinoCoinBalance(newBalance)
            Log.d("DinoRepository", "Spent $amount DinoCoins. Remaining: $newBalance")
        } else {
            Log.w("DinoRepository", "Not enough DinoCoins to spend. Required: $amount, Has: ${profile.dinocoinBalance}")
        }
    }

    suspend fun createInitialProfile() = withContext(Dispatchers.IO) {
        Log.d("DinoRepository", "Профиль не найден. Создание нового профиля...")
        userProfileDao.insertProfile(UserProfile())
        Log.d("DinoRepository", "Новый профиль успешно создан.")
    }

    // --- ОСТАЛЬНЫЕ ПУБЛИЧНЫЕ МЕТОДЫ ---
    fun getAllBones(): Flow<List<Bone>> = boneDao.getAllBones()

    // ДОБАВИЛ ЭТУ ФУНКЦИЮ: addBone
    // Теперь она публичная, чтобы InventoryViewModel мог ее вызывать.
    suspend fun addBone(bone: Bone) { // <-- УДАЛИЛ "private" ЗДЕСЬ
        boneDao.insertBone(bone)
        updateProfileStats()
    }

    fun getUserProfile(): Flow<UserProfile?> = userProfileDao.getProfile()

    suspend fun boneFound(bone: Bone, zoneId: String) {
        addBone(bone) // Теперь эта строка будет работать
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

    // --- ПРИВАТНЫЕ МЕТОДЫ РЕПОЗИТОРИЯ (Остальные, которые должны быть приватными) ---
    // Этот метод был частью изначального "private suspend fun addBone",
    // но теперь BoneGenerator может вызывать публичную addBone выше.
    // Если вам нужна функция для удаления одной кости из группы (это сложнее)
    // suspend fun removeOneBone(boneKey: BoneKey) { ... }
    // Удаляем избыточную приватную версию addBone, чтобы не было дублирования
    // (поскольку публичная addBone теперь делает то же самое).
    // Если же вам нужно, чтобы публичная addBone вызывала приватную, то
    // приватная должна быть переименована, например, в internalAddBone(bone: Bone)
    // и вызываться из публичной. Но в данном случае, она просто должна быть публичной.


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