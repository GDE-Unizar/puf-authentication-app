package es.gde.unizar.puf

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Reads events
 */
class EventReader(cntx: Context) {

    // sensor
    private val sensorManager = cntx.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /**
     * records events
     */
    suspend fun record(sensorType: Int, samples: Int, periodMs: Int, progress: (Int) -> Unit) = suspendCoroutine { continuation ->
        val values = mutableListOf<FloatArray>()

        sensorManager.registerListener(
            object : SensorEventListener {
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.values?.let { values += it.clone() }
                    progress(values.size)
                    if (values.size >= samples) {
                        sensorManager.unregisterListener(this)
                        continuation.resume(values)
                    }
                }
            },
            sensorManager.getDefaultSensor(sensorType) ?: throw Exception("Sensor not found"),
            periodMs * 1000
        )
    }

}