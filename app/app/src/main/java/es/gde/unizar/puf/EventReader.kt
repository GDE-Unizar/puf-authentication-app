package es.gde.unizar.puf

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Reads events
 */
class EventReader(cntx: Context) {

    // sensor
    private val sensorManager = cntx.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /**
     * records events
     */
    fun record(sensorType: Int, samples: Int, periodMs: Int, progress: (Int) -> Unit, callback: (List<FloatArray>) -> Unit) {
        val values = mutableListOf<FloatArray>()

        sensorManager.registerListener(
            object : SensorEventListener {
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.values?.let { values += it }
                    progress(values.size)
                    if (values.size >= samples) {
                        sensorManager.unregisterListener(this)
                        callback(values)
                    }
                }
            },
            sensorManager.getDefaultSensor(sensorType) ?: throw Exception("Sensor not found"),
            periodMs * 1000
        )
    }

}