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
    fun record(sensorType: Int, samples: Int, periodMs: Int, callback: (List<Event>) -> Unit) {
        val values = mutableListOf<Event>()

        sensorManager.registerListener(
            object : SensorEventListener {
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                override fun onSensorChanged(event: SensorEvent?) {
                    values += Event(event?.values)
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

data class Event(val x: Float, val y: Float, val z: Float)

fun Event(values: FloatArray?) = values?.let { Event(values[0], values[1], values[2]) } ?: Event(0f, 0f, 0f)