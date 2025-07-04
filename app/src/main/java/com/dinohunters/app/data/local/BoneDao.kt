// Путь: app/src/main/java/com/dinohunters/app/data/local/BoneDao.kt

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

    /**
     * Возвращает общее количество найденных костей.
     * (У вас уже был этот метод, он корректен)
     */
    @Query("SELECT COUNT(*) FROM bones")
    suspend fun getBonesCount(): Int

    /**
     * [ИЗМЕНЕНО] Возвращает количество костей определенной редкости.
     * Метод переименован с getBonesByRarity на getBonesCountByRarity,
     * чтобы соответствовать вызову в DinoRepository.
     */
    @Query("SELECT COUNT(*) FROM bones WHERE rarity = :rarity")
    suspend fun getBonesCountByRarity(rarity: BoneRarity): Int // <-- Имя изменено здесь
}