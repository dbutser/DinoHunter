// com.dinohunters.app.data.model.BoneKey.kt
package com.dinohunters.app.data.model

// Этот data class будет использоваться как ключ для группировки.
// Он содержит все поля, по которым мы хотим считать кости "одинаковыми",
// кроме уникального ID.
data class BoneKey(
    val name: String,         // "Череп Тираннозавра"
    val type: String,         // "Череп"
    val rarity: BoneRarity,   // LEGENDARY, COMMON и т.д.
    val imageUrl: String      // URL картинки, чтобы группировать только по визуально одинаковым костям
) {
    // Вспомогательная функция для создания BoneKey из Bone
    companion object {
        fun fromBone(bone: Bone): BoneKey {
            return BoneKey(bone.name, bone.type, bone.rarity, bone.imageUrl)
        }
    }
}