package com.dinohunters.app.data.local

import androidx.room.*
import com.dinohunters.app.data.model.Bone
import com.dinohunters.app.data.model.BoneRarity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoneDao {
    @Query("SELECT * FROM bones ORDER BY foundAt DESC")
    fun getAllBones(): Flow<List<Bone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBone(bone: Bone)

    @Delete
    suspend fun deleteBone(bone: Bone)

    @Query("SELECT COUNT(*) FROM bones")
    suspend fun getBonesCount(): Int

    @Query("SELECT COUNT(*) FROM bones WHERE rarity = :rarity")
    suspend fun getBonesByRarity(rarity: BoneRarity): Int
}