package com.dinohunters.app.utils

import com.dinohunters.app.data.model.Bone
import com.dinohunters.app.data.model.BoneRarity
import kotlin.random.Random

// --- Предполагаемые модели данных (если они еще не определены) ---
// data class Bone(
//     val id: String,
//     val name: String,
//     val type: String,
//     val rarity: BoneRarity,
//     val imageUrl: String,
//     val description: String
// )
//
// enum class BoneRarity {
//     COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
// }
// -----------------------------------------------------------------

// Data class для удобства хранения шаблонов костей с их весом
data class BonePartTemplate(
    val name: String,
    val type: String,
    val rarity: BoneRarity,
    val weight: Int // Добавляем поле для веса
)

object BoneGenerator {

    // 10 типов костей с их редкостью и ВЕСОМ
    // Чем выше вес, тем чаще выпадает кость.
    // Я распределил веса так, чтобы легендарные были очень редкими, а обычные - частыми.
    // Можно корректировать эти значения для изменения вероятностей.
    private val bonePartTemplates = listOf(
        // Легендарные (очень редкие)
        BonePartTemplate("ДНК", "ДНК", BoneRarity.LEGENDARY, 1),      // Вес 1 - самый низкий шанс
        // Эпические (редкие)
        BonePartTemplate("Череп", "Череп", BoneRarity.EPIC, 3),            // Вес 3
        BonePartTemplate("Коготь", "Коготь", BoneRarity.EPIC, 3),
        // Редкие
        BonePartTemplate("Зуб", "Зуб", BoneRarity.RARE, 10),             // Вес 10
        BonePartTemplate("Бедренная кость", "Конечность", BoneRarity.RARE, 10),
        BonePartTemplate("Плечевая кость", "Конечность", BoneRarity.RARE, 10),
        // Необычные
        BonePartTemplate("Ребро", "Туловище", BoneRarity.UNCOMMON, 25),    // Вес 25
        BonePartTemplate("Лопатка", "Туловище", BoneRarity.UNCOMMON, 25),
        // Обычные (частые)
        BonePartTemplate("Тазовая кость", "Таз", BoneRarity.COMMON, 50),     // Вес 50 - высокий шанс
        BonePartTemplate("Позвонок", "Позвоночник", BoneRarity.COMMON, 50)
    )

    // 10 видов динозавров (в родительном падеже)
    private val dinosaurNames = listOf(
        "Тираннозавра",
        "Трицератопса",
        "Велоцираптора",
        "Брахиозавра",
        "Стегозавра",
        "Спинозавра",
        "Аллозавра",
        "Анкилозавра",
        "Птеродактиля",
        "Дейнониха"
    )

    // Вычисляем общую сумму всех весов для удобства выбора
    private val totalWeight = bonePartTemplates.sumOf { it.weight }

    fun generateRandomBone(): Bone {
        // --- Логика взвешенного выбора шаблона кости ---
        var randomValue = Random.nextInt(totalWeight) // Генерируем случайное число от 0 до (totalWeight - 1)

        val selectedBonePart: BonePartTemplate =
            bonePartTemplates.first { template ->
                // Отнимаем вес текущего шаблона от случайного значения
                // Если randomValue становится <= 0, значит, мы попали в диапазон этого шаблона
                randomValue -= template.weight
                randomValue < 0
            }

        val bonePartName = selectedBonePart.name
        val boneType = selectedBonePart.type
        val boneRarity = selectedBonePart.rarity
        // ------------------------------------------------

        // Случайно выбираем имя динозавра (это по-прежнему равномерно)
        val selectedDinosaurName = dinosaurNames.random()

        // Соединяем их для полного названия кости
        val boneName = "$bonePartName $selectedDinosaurName"

        // Генерируем уникальный ID
        val id = "bone_${System.currentTimeMillis()}_${Random.nextInt(10000)}"

        // Формируем описание на основе редкости
        val rarityDescription = when (boneRarity) {
            BoneRarity.LEGENDARY -> "уникальная и крайне редкая"
            BoneRarity.EPIC -> "очень редкая"
            BoneRarity.RARE -> "редкая"
            BoneRarity.UNCOMMON -> "необычная"
            BoneRarity.COMMON -> "обычная"
        }

        // Возвращаем новую кость
        return Bone(
            id = id,
            name = boneName,
            type = boneType,
            rarity = boneRarity,
            imageUrl = "https://i.imgur.com/xwweStJ.png",
            description = "Древняя кость динозавра, найденная в ходе исследований. Это ${boneName} - ${rarityDescription} находка!"
        )
    }
}