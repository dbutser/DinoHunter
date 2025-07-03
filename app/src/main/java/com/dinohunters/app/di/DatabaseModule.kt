package com.dinohunters.app.di

import android.content.Context
import androidx.room.Room
import com.dinohunters.app.data.local.BoneDao
import com.dinohunters.app.data.local.BoneZoneDao
import com.dinohunters.app.data.local.DinoDatabase
import com.dinohunters.app.data.local.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DinoDatabase {
        return Room.databaseBuilder(
            context,
            DinoDatabase::class.java,
            "dino_database"
        ).build()
    }

    @Provides
    fun provideBoneDao(database: DinoDatabase): BoneDao = database.boneDao()

    @Provides
    fun provideBoneZoneDao(database: DinoDatabase): BoneZoneDao = database.boneZoneDao()

    @Provides
    fun provideUserProfileDao(database: DinoDatabase): UserProfileDao = database.userProfileDao()
}