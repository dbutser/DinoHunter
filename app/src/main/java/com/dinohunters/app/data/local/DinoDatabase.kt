package com.dinohunters.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dinohunters.app.data.model.Bone
import com.dinohunters.app.data.model.BoneZone
import com.dinohunters.app.data.model.UserProfile

@Database(
    entities = [Bone::class, BoneZone::class, UserProfile::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DinoDatabase : RoomDatabase() {
    abstract fun boneDao(): BoneDao
    abstract fun boneZoneDao(): BoneZoneDao
    abstract fun userProfileDao(): UserProfileDao
}