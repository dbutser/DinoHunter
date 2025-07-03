package com.dinohunters.app.utils

import com.dinohunters.app.data.model.Bone
import com.dinohunters.app.data.model.BoneRarity
import kotlin.random.Random

object BoneGenerator {
    private val boneTemplates = listOf(
        Triple("Череп тираннозавра", "Череп", BoneRarity.LEGENDARY),
        Triple("Зуб велоцираптора", "Зуб", BoneRarity.RARE),
        Triple("Бедренная кость трицератопса", "Конечность", BoneRarity.EPIC),
        Triple("Ребро диплодока", "Туловище", BoneRarity.UNCOMMON),
        Triple("Коготь дейнониха", "Коготь", BoneRarity.RARE),
        Triple("Позвонок стегозавра", "Позвоночник", BoneRarity.COMMON)
    )

    fun generateRandomBone(): Bone {
        val template = boneTemplates.random()
        val id = "bone_${System.currentTimeMillis()}_${Random.nextInt(10000)}"
        return Bone(
            id = id,
            name = template.first,
            type = template.second,
            rarity = template.third,
            imageUrl = "https://loremflickr.com/320/240/dinosaur,bone/all?lock=${Random.nextInt(100)}",
            description = "Древняя кость динозавра, найденная в ходе исследований. ${template.first} - редкая находка!"
        )
    }
}