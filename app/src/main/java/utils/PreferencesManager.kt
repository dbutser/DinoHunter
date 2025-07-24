// Путь: app/src/main/java/com/dinohunters/app/utils/PreferencesManager.kt

package com.dinohunters.app.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

// Определяем DataStore для хранения настроек
// Это расширение для Context, которое создает/получает DataStore
val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton // Указываем Hilt, что это синглтон, будет один экземпляр на все приложение
class PreferencesManager @Inject constructor(@ApplicationContext private val context: Context) {

    // Ключ, по которому будет храниться дата последней генерации зон
    private val LAST_ZONE_GENERATION_DATE_KEY = longPreferencesKey("last_zone_generation_date")

    /**
     * Сохраняет текущую дату (в виде Epoch Day - количество дней с 1970 года)
     * как дату последней генерации зон.
     */
    suspend fun setLastZoneGenerationDateToNow() {
        val todayEpochDay = LocalDate.now().toEpochDay() // Получаем текущий день как число
        context.dataStore.edit { settings ->
            settings[LAST_ZONE_GENERATION_DATE_KEY] = todayEpochDay // Сохраняем это число
        }
    }

    /**
     * Получает дату последней генерации зон из настроек.
     * Возвращает null, если дата еще не была установлена.
     */
    suspend fun getLastZoneGenerationDate(): LocalDate? {
        // Получаем сохраненное число дней. .first() делает поток Flow синхронным запросом.
        val epochDay = context.dataStore.data.first()[LAST_ZONE_GENERATION_DATE_KEY]
        // Если число есть, преобразуем его обратно в LocalDate, иначе возвращаем null
        return epochDay?.let { LocalDate.ofEpochDay(it) }
    }
}