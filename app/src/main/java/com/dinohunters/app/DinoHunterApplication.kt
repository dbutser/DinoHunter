// Путь: app/src/main/java/com/dinohunters/app/DinoHunterApplication.kt

package com.dinohunters.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class DinoHunterApplication : Application(), Configuration.Provider {

    // Внедряем HiltWorkerFactory, чтобы WorkManager мог создавать наших "рабочих" с зависимостями
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // --- [ИЗМЕНЕНИЕ ЗДЕСЬ] ---
    // Вместо функции getWorkManagerConfiguration() мы реализуем свойство workManagerConfiguration
    // с помощью ключевого слова override val и геттера get().
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO) // Добавляем логирование для отладки
            .build()
    // --- КОНЕЦ ИЗМЕНЕНИЯ ---

    override fun onCreate() {
        super.onCreate()
        // Запускаем планировщик при первом старте приложения
        scheduleDailyCleanup()
    }

    private fun scheduleDailyCleanup() {
        // Рассчитываем задержку до ближайшей полуночи
        val currentTime = Calendar.getInstance()
        val dueTime = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1) // всегда планируем на завтрашнюю полночь для простоты
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val initialDelay = dueTime.timeInMillis - currentTime.timeInMillis


    }
}