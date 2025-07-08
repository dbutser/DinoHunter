package com.dinohunters.app.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Calendar

private val Context.coinDataStore by preferencesDataStore(name = "coin_prefs")

@Singleton
class DinoCoinRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val dataStore = context.coinDataStore
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val BALANCE_KEY = intPreferencesKey("balance")
    private val LAST_STEPS_KEY = floatPreferencesKey("last_steps")
    private val LAST_DAY_KEY = intPreferencesKey("last_day")

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _coinBalance = MutableStateFlow(0)
    val coinBalance: StateFlow<Int> = _coinBalance.asStateFlow()

    init {
        scope.launch {
            dataStore.data.map { it[BALANCE_KEY] ?: 0 }.collect { _coinBalance.value = it }
        }
        stepSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val currentSteps = event.values[0]
        scope.launch {
            val prefs = dataStore.data.first()
            var lastDay = prefs[LAST_DAY_KEY] ?: today
            var lastSteps = prefs[LAST_STEPS_KEY] ?: currentSteps
            var balance = prefs[BALANCE_KEY] ?: 0

            if (today != lastDay) {
                lastDay = today
                lastSteps = currentSteps
            }

            val delta = (currentSteps - lastSteps).toInt()
            if (delta > 0) {
                lastSteps = currentSteps
                balance += delta
                dataStore.edit {
                    it[LAST_DAY_KEY] = lastDay
                    it[LAST_STEPS_KEY] = lastSteps
                    it[BALANCE_KEY] = balance
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun spendCoins(amount: Int) {
        scope.launch {
            dataStore.edit { prefs ->
                val current = prefs[BALANCE_KEY] ?: 0
                prefs[BALANCE_KEY] = (current - amount).coerceAtLeast(0)
            }
        }
    }
}
