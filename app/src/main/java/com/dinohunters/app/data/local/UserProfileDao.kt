package com.dinohunters.app.data.local

import androidx.room.*
import com.dinohunters.app.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)

    // --- ДОБАВЛЕНО ---
    @Query("UPDATE user_profile SET dinocoinBalance = :newBalance, lastSyncedSteps = :newSyncedSteps WHERE id = 1")
    suspend fun updateDinoCoinsAndSteps(newBalance: Long, newSyncedSteps: Long)

    @Query("UPDATE user_profile SET dinocoinBalance = :newBalance WHERE id = 1")
    suspend fun updateDinoCoinBalance(newBalance: Long)
}