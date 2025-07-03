package com.dinohunters.app.data.local

import androidx.room.*
import com.dinohunters.app.data.model.BoneZone
import kotlinx.coroutines.flow.Flow

@Dao
interface BoneZoneDao {
    @Query("SELECT * FROM bone_zones")
    fun getAllZones(): Flow<List<BoneZone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: BoneZone)

    @Update
    suspend fun updateZone(zone: BoneZone)

    @Query("SELECT * FROM bone_zones WHERE isCollected = 0")
    suspend fun getUnCollectedZones(): List<BoneZone>

    @Query("UPDATE bone_zones SET isCollected = 1, collectedAt = :timestamp WHERE id = :zoneId")
    suspend fun markZoneCollected(zoneId: String, timestamp: Long)
}