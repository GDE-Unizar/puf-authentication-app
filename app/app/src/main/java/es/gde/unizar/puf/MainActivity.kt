package es.gde.unizar.puf

import android.hardware.Sensor
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import es.gde.unizar.puf.databinding.ActivityMainBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
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
            lifecycleScope.launch {
                val timerMillis = 5000
                val samples = 1000
                val periodMillis = 10

                var pendingMillis = timerMillis + samples * periodMillis * 2
                var currentMillis = 0
                progress("Starting process (${pendingMillis / 1000}s)", value = 0, full = pendingMillis)

                // wait 5 seconds to stabilize
                Timer(timerMillis, 500) {
                    pendingMillis -= 500
                    currentMillis += 500
                    progress("Stabilizing (${pendingMillis / 1000}s)", value = currentMillis, secondary = timerMillis)
                }

                // read no vibration
                val (accelerometer, gyroscope) = awaitAll(async {
                    eventReader.record(accelerometerSensor, samples, periodMillis) {
                        pendingMillis -= periodMillis
                        currentMillis += periodMillis
                        progress("Reading accelerometer + gyroscope (${pendingMillis / 1000}s)", value = currentMillis, secondary = timerMillis + samples * periodMillis)
                    }
                }, async {
                    eventReader.record(gyroscopeSensor, samples, periodMillis) {
                        // concurrent
                    }
                })

                // read vibration
                vibrator.vibrate()
                val accelVibrate = eventReader.record(accelerometerSensor, samples, periodMillis) {
                    pendingMillis -= periodMillis
                    currentMillis += periodMillis
                    progress("Reading accelerometer with vibration (${pendingMillis / 1000}s)", value = currentMillis, secondary = timerMillis + samples * periodMillis * 2)
                }
                vibrator.stop()

                progress("Calculating key", full = 0)

                // debug
                //listOf("ACCELEROMETER" to accelerometer, "ACCELEROMETER VIBRATION" to acceleVibrate, "GYROSCOPE" to gyroscope).flatMap { (label, data) -> listOf(label) + data.map { it.joinToString("\t") } }.forEachIndexed { index, s ->
                //    Log.d("PUF_SENSOR_RESULT", "$index $s")
                //    Thread.sleep(1)
                //}

                // process
                val key = processor.main(
                    listOf(
                        Step(accelerometer.sensor2process, Operation.NOISE, 10, 3, 3, 0, samples, gravity),
                        Step(accelerometer.sensor2process, Operation.AVER, 10, 3, 2, 0, samples, gravity),
                        Step(accelVibrate.sensor2process, Operation.NOISE, 30, 1, 2, 0, samples, gravity),
                        Step(accelVibrate.sensor2process, Operation.AVER, 30, 1, 2, 0, samples, gravity),
                        Step(gyroscope.sensor2process, Operation.NOISE, 10, 3, 4, 0, samples, gravity),
                        Step(gyroscope.sensor2process, Operation.AVER, 10, 3, 5, 0, samples, gravity),
                    )
                )
                Log.d("PUF_SENSOR_RESULT", "KEY=$key")

                // set
                showKey(key)
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