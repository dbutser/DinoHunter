package com.dinohunters.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Используем Int для единственного профиля
    val nickname: String = "Охотник",
    val totalBones: Int = 0,
    val commonBones: Int = 0,
    val uncommonBones: Int = 0,
    val rareBones: Int = 0,
    val epicBones: Int = 0,
    val legendaryBones: Int = 0,
    val sessionBones: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),

    // --- ДОБАВЛЕННЫЕ ПОЛЯ ---
    val dinocoinBalance: Long = 0,
    val lastSyncedSteps: Long = 0
)