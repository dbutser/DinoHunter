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
    private val googleApiKey = "YOUR_GOOGLE_API_KEY" // TODO: Замените на ваш ключ API

    companion object {
        private const val HINT_HIGHLIGHT_COST = 1000L // Стоимость подсказки "Подсветка"
        private const val HINT_REMOTE_COLLECT_COST = 2500L // Стоимость подсказки "Удаленный сбор"
    }

    // --- ИГРОВЫЕ КОНСТАНТЫ ---
    private val ZONE_RADIUS_METERS = 100.0
    private val MINIMUM_DISTANCE_BETWEEN_ZONES_METERS = 210.0
    private val PLAYER_AURA_RADIUS_METERS = 1500.0
    private val ZONES_TO_GENERATE_COUNT = 15
    private val MAX_GENERATION_ATTEMPTS = 500

    // --- ЛОГИКА ДИНОКОИНОВ И ПРОФИЛЯ ---
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
            Log.e("DinoRepository", "Ошибка: Не удалось получить данные с датчика шагов (вернулся null).")
            return@withContext
        }
        Log.d("DinoRepository", "Данные с датчика получены. Всего шагов с перезагрузки: $totalStepsFromSensor")

        val newSteps = totalStepsFromSensor - profile.lastSyncedSteps
        Log.d("DinoRepository", "Вычислены новые шаги: $newSteps")

        if (newSteps > 0) {
            val newBalance = profile.dinocoinBalance + newSteps
            userProfileDao.updateDinoCoinsAndSteps(newBalance, totalStepsFromSensor)
            Log.d("DinoRepository", "УСПЕХ! Начислено $newSteps динокоинов. Новый баланс: $newBalance.")
        } else {
            Log.w("DinoRepository", "Новых шагов для начисления нет.")
        }
        Log.d("DinoRepository", "--- Синхронизация завершена ---")
    }

    suspend fun createInitialProfile() = withContext(Dispatchers.IO) {
        if (userProfileDao.getProfile().firstOrNull() == null) {
            Log.d("DinoRepository", "Профиль не найден. Создание нового профиля...")
            userProfileDao.insertProfile(UserProfile())
            Log.d("DinoRepository", "Новый профиль успешно создан.")
        }
    }

    fun getUserProfile(): Flow<UserProfile?> = userProfileDao.getProfile()

    // --- ЛОГИКА ПОДСКАЗОК ---
    suspend fun purchaseHighlightHint(): Boolean = withContext(Dispatchers.IO) {
        val profile = userProfileDao.getProfile().firstOrNull()
            ?: return@withContext false

        if (profile.dinocoinBalance >= HINT_HIGHLIGHT_COST) {
            val newBalance = profile.dinocoinBalance - HINT_HIGHLIGHT_COST
            userProfileDao.updateDinoCoinBalance(newBalance)
            Log.d("DinoRepository", "Подсказка 'Подсветка' куплена за $HINT_HIGHLIGHT_COST. Новый баланс: $newBalance")
            true
        } else {
            Log.w("DinoRepository", "Недостаточно средств для покупки подсказки. Требуется: $HINT_HIGHLIGHT_COST, в наличии: ${profile.dinocoinBalance}")
            false
        }
    }

    /**
     * [НОВЫЙ МЕТОД] Пытается купить подсказку "Удаленный сбор".
     * @return true, если покупка удалась, иначе false.
     */
    suspend fun purchaseRemoteCollectHint(): Boolean = withContext(Dispatchers.IO) {
        val profile = userProfileDao.getProfile().firstOrNull()
            ?: return@withContext false

        if (profile.dinocoinBalance >= HINT_REMOTE_COLLECT_COST) {
            val newBalance = profile.dinocoinBalance - HINT_REMOTE_COLLECT_COST
            userProfileDao.updateDinoCoinBalance(newBalance)
            Log.d("DinoRepository", "Подсказка 'Удаленный сбор' куплена за $HINT_REMOTE_COLLECT_COST. Новый баланс: $newBalance")
            true
        } else {
            Log.w("DinoRepository", "Недостаточно средств для покупки подсказки 'Удаленный сбор'. Требуется: $HINT_REMOTE_COLLECT_COST, в наличии: ${profile.dinocoinBalance}")
            false
        }
    }


    // --- ОСНОВНАЯ ИГРОВАЯ ЛОГИКА ---
    fun getAllBones(): Flow<List<Bone>> = boneDao.getAllBones()

    suspend fun addBone(bone: Bone) {
        boneDao.insertBone(bone)
        updateProfileStats()
    }

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
        if (boneZoneDao.getAllZonesList().isEmpty()) {
            generateNewZonesAround(playerLocation)
            return@withContext true
        }
        return@withContext false
    }

    suspend fun clearAllBoneData() = withContext(Dispatchers.IO) {
        boneZoneDao.deleteAllZones()
        // TODO: Добавить метод `clearAll()` или `deleteAll()` в интерфейс `BoneDao` и раскомментировать строку ниже
        // boneDao.clearAll()
        Log.d("DinoRepository", "All zone data has been cleared.")
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
        Log.d("DinoRepository", "Статистика профиля обновлена. Всего костей: $total")
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
                Log.e("DinoRepository", "Ошибка запроса к Roads API", e)
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

        Log.d("DinoRepository", "Сгенерировано ${validZonesToSave.size} новых зон.")
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