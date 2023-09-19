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
            setPage(Page.WAIT) // TODO: real progress

            // read
            // TODO: place this in a sequential way
            eventReader.record(accelerometerSensor, 50, 100) { accelerometer ->
                vibrator.vibrate()
                eventReader.record(accelerometerSensor, 50, 100) { accelerometerVibration ->
                    vibrator.stop()
                    eventReader.record(gyroscopeSensor, 50, 100) { gyroscope ->

                        // compute
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
                        binding.txtKey.text = key
                        setPage(Page.KEY)
                    }
                }
            }
        }
        binding.btnReset.setOnClickListener {
            setPage(Page.START)
        }

        // start
        setPage(Page.START)
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

private val List<Event>.sensor2process
    get() = map { listOf(it.x, it.y, it.z).map { it.toDouble() } }

val gravity = listOf(0.0, 0.0, 9.8)

private val uncalibratedAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && "sdk" !in Build.PRODUCT
private val accelerometerSensor = if (uncalibratedAvailable) Sensor.TYPE_ACCELEROMETER_UNCALIBRATED else Sensor.TYPE_ACCELEROMETER
private val gyroscopeSensor = if (uncalibratedAvailable) Sensor.TYPE_GYROSCOPE_UNCALIBRATED else Sensor.TYPE_GYROSCOPE