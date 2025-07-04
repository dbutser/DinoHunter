// Путь: app/src/main/java/com/dinohunters/app/data/local/BoneZoneDao.kt

package com.dinohunters.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dinohunters.app.data.model.BoneZone
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для работы с зонами раскопок (BoneZone).
 */
@Dao
interface BoneZoneDao {

    /**
     * Возвращает все зоны раскопок из базы данных в виде потока данных.
     * Используется для реактивного обновления UI.
     */
    @Query("SELECT * FROM bone_zones")
    fun getAllZones(): Flow<List<BoneZone>>

    /**
     * Вставляет одну зону раскопок в базу. Если зона с таким id уже существует,
     * она будет заменена.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: BoneZone)

    /**
     * Вставляет список зон раскопок в базу.
     * Это гораздо эффективнее, чем вставлять их по одной в цикле.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(zones: List<BoneZone>)

    /**
     * Обновляет существующую зону в базе данных.
     */
    @Update
    suspend fun updateZone(zone: BoneZone)

    /**
     * Возвращает список всех еще не "собранных" зон.
     */
    @Query("SELECT * FROM bone_zones WHERE isCollected = 0")
    suspend fun getUnCollectedZones(): List<BoneZone>

    /**
     * Помечает зону как "собранную", устанавливая флаг isCollected и время сбора.
     */
    @Query("UPDATE bone_zones SET isCollected = 1, collectedAt = :timestamp WHERE id = :zoneId")
    suspend fun markZoneCollected(zoneId: String, timestamp: Long)


    // --- ДОБАВЛЕННЫЕ МЕТОДЫ ДЛЯ НОВОЙ ЛОГИКИ ---

    /**
     * [НОВЫЙ] Возвращает список всех зон в виде обычного List (не Flow).
     * Нужен для синхронной проверки на пересечения при генерации.
     */
    @Query("SELECT * FROM bone_zones")
    suspend fun getAllZonesList(): List<BoneZone>

    /**
     * [НОВЫЙ] Получает список зон в заданном "квадрате" координат.
     * Это быстрая операция для предварительной фильтрации зон в "ауре" игрока.
     */
    @Query("SELECT * FROM bone_zones WHERE centerLat BETWEEN :minLat AND :maxLat AND centerLng BETWEEN :minLon AND :maxLon")
    suspend fun getZonesInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<BoneZone>

    /**
     * [НОВЫЙ] Удаляет АБСОЛЮТНО все записи из таблицы.
     * Используется для ежедневной "Великой Чистки".
     */
    @Query("DELETE FROM bone_zones")
    suspend fun deleteAllZones()
}