package com.dinohunters.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "bones")
data class Bone(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val rarity: BoneRarity,
    val imageUrl: String,
    val description: String,
    val foundAt: Long = System.currentTimeMillis()
) : Parcelable

enum class BoneRarity(val displayName: String, val color: Long) {
    COMMON("Обычная", 0xFF8BC34A),
    UNCOMMON("Необычная", 0xFF2196F3),
    RARE("Редкая", 0xFF9C27B0),
    EPIC("Эпическая", 0xFFFF9800),
    LEGENDARY("Легендарная", 0xFFFF5722)
}