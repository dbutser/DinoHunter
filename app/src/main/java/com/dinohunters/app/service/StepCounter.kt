package com.dinohunters.app.service

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Класс для работы с датчиком шагов устройства.
 * Предоставляет Flow, который эмитит общее количество шагов с последней перезагрузки.
 */
@Singleton
class StepCounter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    /**
     * Проверяет, есть ли на устройстве датчик шагов.
     */
    fun hasStepCounterSensor(): Boolean {
        return stepSensor != null
    }

    /**
     * Проверяет, предоставлено ли разрешение на чтение данных об активности.
     */
    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Для версий Android до 10 разрешение не требуется
            true
        }
    }

    /**
     * Возвращает Flow, который эмитит текущее значение счетчика шагов.
     * Если датчика нет или разрешение не предоставлено, Flow завершится без значений.
     */
    fun getTotalSteps(): Flow<Long> = callbackFlow {
        if (!hasStepCounterSensor() || !hasPermission()) {
            close(IllegalStateException("Датчик шагов недоступен или разрешение не предоставлено"))
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // event.values[0] - это float, содержащий общее количество шагов с момента перезагрузки.
                    trySend(it.values[0].toLong())
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Не используется в данном контексте
            }
        }

        sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)

        // Этот блок кода выполнится, когда Flow будет закрыт (когда корутина-потребитель отменится)
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}