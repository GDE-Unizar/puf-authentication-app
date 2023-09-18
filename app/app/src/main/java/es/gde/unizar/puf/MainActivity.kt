package es.gde.unizar.puf

import android.hardware.Sensor
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import es.gde.unizar.puf.databinding.ActivityMainBinding
import kotlin.concurrent.thread


private val List<Event>.convert: List<List<Double>>
    get() = map { listOf(it.x, it.y, it.z).map { it.toDouble() } }

val gravity = listOf(0.0, 0.0, 9.8)

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

        // test
        thread {
            val ok = processor.test()
            runOnUiThread {
                Toast.makeText(this, if (ok) "Test OK" else "Expected value differ!", Toast.LENGTH_SHORT).show()
            }
        }

        // config
        binding.btnGet.setOnClickListener {
            setPage(Page.WAIT)

            // read
            eventReader.record(Sensor.TYPE_ACCELEROMETER, 50, 100) { accelerometer1 ->

                eventReader.record(Sensor.TYPE_ACCELEROMETER, 50, 100) { accelerometer2 ->
                    eventReader.record(Sensor.TYPE_ACCELEROMETER, 50, 100) { accelerometer3 ->

                        // compute
                        val key = processor.main(
                            listOf(
                                Step(accelerometer1.convert, Operation.NOISE, 10, 3, 3, 500, 2000, gravity),
                                Step(accelerometer1.convert, Operation.AVER, 10, 3, 2, 500, 2000, gravity),
                                Step(accelerometer2.convert, Operation.NOISE, 30, 1, 2, 500, 2000, gravity),
                                Step(accelerometer2.convert, Operation.AVER, 30, 1, 2, 500, 2000, gravity),
                                Step(accelerometer3.convert, Operation.NOISE, 10, 3, 4, 500, 2000, gravity),
                                Step(accelerometer3.convert, Operation.AVER, 10, 3, 5, 500, 2000, gravity),
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

    private enum class Page { START, WAIT, KEY }

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