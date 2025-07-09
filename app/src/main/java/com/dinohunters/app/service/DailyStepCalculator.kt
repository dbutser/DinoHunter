// com/dinohunters/app/data/DailyStepCalculator.kt
package com.dinohunters.app.data // Или package com.dinohunters.app.data.repository

import android.content.SharedPreferences
import com.dinohunters.app.service.StepCounter // Ваш класс StepCounter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Класс, который использует StepCounter для подсчета шагов за текущий день.
 * Обрабатывает сброс счетчика при перезагрузке и смену дня.
 */
@Singleton
class DailyStepCalculator @Inject constructor(
    private val stepCounter: StepCounter, // Ваш класс StepCounter
    private val sharedPreferences: SharedPreferences // Для сохранения базовых значений
) {
    // StateFlow, который будет эмитить количество шагов за сегодня
    private val _stepsToday = MutableStateFlow(0)
    val stepsToday = _stepsToday.asStateFlow()

    // Базовое значение шагов (количество шагов с датчика на начало текущего дня)
    private var baselineSteps: Long = 0L

    // CoroutineScope для обработки Flow, чтобы он был активен пока жив синглтон
    private val scope = CoroutineScope(Dispatchers.IO + Job()) // Добавляем Job для контроля

    init {
        // Загружаем сохраненные базовые значения при инициализации
        loadBaseline()
        // Начинаем слушать Flow из StepCounter
        startListeningForSteps()
    }

    private fun startListeningForSteps() {
        // Мы используем launchIn с созданным scope, чтобы Flow слушал постоянно
        // пока жив синглтон DailyStepCalculator.
        stepCounter.getTotalSteps()
            .onEach { totalStepsFromReboot ->
                val todayDate = getTodayDateString()
                val savedDate = sharedPreferences.getString(KEY_BASELINE_DATE, "")

                // Логика обработки:
                // 1. Если наступил новый день
                // 2. Если количество шагов с датчика меньше базового (означает перезагрузку устройства)
                if (todayDate != savedDate) {
                    // Новый день: Обновляем базовое значение на текущее показание датчика
                    baselineSteps = totalStepsFromReboot
                    saveBaseline(baselineSteps, todayDate)
                } else if (totalStepsFromReboot < baselineSteps) {
                    // Перезагрузка: Обновляем базовое значение на текущее показание датчика
                    // (Датчик сбросился до 0 или близкого к 0 значению)
                    baselineSteps = totalStepsFromReboot
                    saveBaseline(baselineSteps, todayDate)
                }
                // Если ни новый день, ни перезагрузка, то baselineSteps остается тем же, что и был.

                // Вычисляем шаги за сегодня
                _stepsToday.value = (totalStepsFromReboot - baselineSteps).toInt()
            }
            .launchIn(scope) // Запускаем сбор Flow в нашем CoroutineScope
    }

    private fun loadBaseline() {
        baselineSteps = sharedPreferences.getLong(KEY_BASELINE_STEPS, 0L)
        val savedDate = sharedPreferences.getString(KEY_BASELINE_DATE, "")
        val todayDate = getTodayDateString()

        // Если при загрузке обнаруживаем, что дата изменилась с последнего сохранения,
        // то сбрасываем _stepsToday в 0 и готовимся к новому дню.
        if (todayDate != savedDate) {
            _stepsToday.value = 0
            // Принудительно сохраняем сегодняшнюю дату, чтобы следующее totalStepsFromReboot
            // стало новым baseline. Baseline 0L на этом этапе - это заглушка,
            // пока не придет первое событие от сенсора.
            // Реальный baseline будет totalStepsFromReboot при первом onSensorChanged
            saveBaseline(0L, todayDate)
        }
    }

    private fun saveBaseline(steps: Long, date: String) {
        sharedPreferences.edit().apply {
            putLong(KEY_BASELINE_STEPS, steps)
            putString(KEY_BASELINE_DATE, date)
            apply()
        }
    }

    private fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    companion object {
        const val KEY_BASELINE_STEPS = "daily_steps_baseline_total"
        const val KEY_BASELINE_DATE = "daily_steps_baseline_date"
    }
}