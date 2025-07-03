package com.dinohunters.app.data.local

import androidx.room.TypeConverter
import com.dinohunters.app.data.model.BoneRarity

class Converters {
    @TypeConverter
    fun fromBoneRarity(rarity: BoneRarity): String = rarity.name

    @TypeConverter
    fun toBoneRarity(rarity: String): BoneRarity = BoneRarity.valueOf(rarity)
}