// Путь: app/src/main/java/com/dinohunters/app/data/model/BoneZone.kt

package com.dinohunters.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Описывает зону раскопок на карте.
 * Это класс данных, который также является таблицей в базе данных Room.
 *
 * @param id Уникальный идентификатор зоны.
 * @param centerLat Широта центра круга на карте.
 * @param centerLng Долгота центра круга на карте.
 * @param radius Радиус круга в метрах. Теперь это обязательное поле.
 * @param hiddenPointLat "Секретная" точка внутри круга, где спрятана кость.
 * @param hiddenPointLng "Секретная" точка внутри круга, где спрятана кость.
 * @param isCollected Флаг, показывающий, была ли найдена кость в этой зоне.
 * @param collectedAt Временная метка (timestamp), когда кость была найдена.
 */
@Entity(tableName = "bone_zones") // <-- Я поменял имя таблицы на bone_zones, как в твоем коде
data class BoneZone(
    @PrimaryKey val id: String,
    val centerLat: Double,
    val centerLng: Double,
    val radius: Double, // <-- САМОЕ ГЛАВНОЕ: убрали значение по умолчанию
    val hiddenPointLat: Double,
    val hiddenPointLng: Double,
    val isCollected: Boolean = false,
    val collectedAt: Long? = null
)