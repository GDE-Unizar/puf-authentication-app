package es.gde.unizar.puf

import android.hardware.Sensor
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import es.gde.unizar.puf.databinding.ActivityMainBinding
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // layout
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // beans
        val processor = KeyProcessor(this)
        val eventReader = EventReader(this)
        val vibrator = Vibrator(this)

        // test
        thread {
            val ok = processor.test()
            runOnUiThread {
                Toast.makeText(this, if (ok) "Test OK" else "Expected value differ!", Toast.LENGTH_SHORT).show()
            }
        }

        // config
        binding.btnGet.setOnClickListener {
            val samples = 2500
            progress("Starting process", value = 0, full = 2500 * 3)

            // read
            // TODO: place this in a sequential way
            eventReader.record(accelerometerSensor, samples, 10, {
                progress("Reading accelerometer", value = it, secondary = samples)
            }) { accelerometer ->
                vibrator.vibrate()
                eventReader.record(accelerometerSensor, 2500, 10, {
                    progress("Reading accelerometer with vibration", value = 2500 + it, secondary = 2500 * 2)
                }) { accelerometerVibration ->
                    vibrator.stop()
                    eventReader.record(gyroscopeSensor, 2500, 10, {
                        progress("Reading gyroscope", value = 2500 * 2 + it, secondary = 2500 * 3)
                    }) { gyroscope ->

                        // compute
                        progress("Calculating key", full = 0)
                        val key = processor.main(
                            listOf(
                                Step(accelerometer.sensor2process, Operation.NOISE, 10, 3, 3, 500, 2000, gravity),
                                Step(accelerometer.sensor2process, Operation.AVER, 10, 3, 2, 500, 2000, gravity),
                                Step(accelerometerVibration.sensor2process, Operation.NOISE, 30, 1, 2, 500, 2000, gravity),
                                Step(accelerometerVibration.sensor2process, Operation.AVER, 30, 1, 2, 500, 2000, gravity),
                                Step(gyroscope.sensor2process, Operation.NOISE, 10, 3, 4, 500, 2000, gravity),
                                Step(gyroscope.sensor2process, Operation.AVER, 10, 3, 5, 500, 2000, gravity),
                            )
                        )

                        // set
                        showKey(key)
                    }
                }
            }
        }
        binding.btnReset.setOnClickListener { reset() }

        // start
        setPage(Page.START)
    }

    private fun reset() {
        setPage(Page.START)
    }

    private fun progress(text: String, value: Int? = null, secondary: Int? = null, full: Int? = null) {
        setPage(Page.WAIT)
        binding.progressText.text = text
        binding.progressBar.apply {
            full?.let {
                max = it
                isIndeterminate = it <= 0
            }
            value?.let { progress = it }
            secondary?.let { secondaryProgress = it }
        }
    }

    private fun showKey(key: String) {
        binding.txtKey.text = key
        setPage(Page.KEY)
    }

    private fun setPage(page: Page) {
        listOf(
            binding.pageStart to Page.START,
            binding.pageWait to Page.WAIT,
            binding.pageKey to Page.KEY,
        ).forEach { (view, withPage) ->
            view.visibility = if (page == withPage) View.VISIBLE else View.GONE
        }
    }
}

private enum class Page { START, WAIT, KEY }

private val List<FloatArray>.sensor2process get() = map { it.map { it.toDouble() } }

val gravity = listOf(0.0, 0.0, 9.8)

private val uncalibratedAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && "sdk" !in Build.PRODUCT
private val accelerometerSensor = if (uncalibratedAvailable) Sensor.TYPE_ACCELEROMETER_UNCALIBRATED else Sensor.TYPE_ACCELEROMETER
private val gyroscopeSensor = if (uncalibratedAvailable) Sensor.TYPE_GYROSCOPE_UNCALIBRATED else Sensor.TYPE_GYROSCOPE