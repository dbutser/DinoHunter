package com.dinohunters.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bone_zones")
data class BoneZone(
    @PrimaryKey val id: String,
    val centerLat: Double,
    val centerLng: Double,
    val radius: Double = 100.0, // meters
    val hiddenPointLat: Double,
    val hiddenPointLng: Double,
    val isCollected: Boolean = false,
    val collectedAt: Long? = null
)