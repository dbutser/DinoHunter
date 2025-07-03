// Путь: app/src/main/java/com/dinohunters/app/data/local/BoneZoneDao.kt

package com.dinohunters.app.data.local

import androidx.room.*
import com.dinohunters.app.data.model.BoneZone
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для работы с зонами раскопок (BoneZone).
 */
@Dao
interface BoneZoneDao {

    /**
     * Возвращает все зоны раскопок из базы данных в виде потока данных.
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
     * НОВАЯ ФУНКЦИЯ: Вставляет список зон раскопок в базу.
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
}