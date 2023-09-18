package es.gde.unizar.puf

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

        // test
        thread {
            val ok = processor.test()
            runOnUiThread {
                Toast.makeText(this, if (ok) "Test OK" else "Expected value differ!", Toast.LENGTH_SHORT).show()
            }
        }

        // config
        binding.btnGet.setOnClickListener {
//            binding.txtKey.text = processor.main(R.raw.example, R.raw.example2, R.raw.example3)
//            setPage(Page.KEY)
//            Log.d("GOT", binding.txtKey.text.toString())
//            Log.d("EXPECTED", EXPECTED)
//            if (binding.txtKey.text.toString() != EXPECTED) Toast.makeText(this, "Different!", Toast.LENGTH_SHORT).show()
        }
        binding.btnReset.setOnClickListener {
            setPage(Page.START)
        }

        // start
        setPage(Page.START)
    }

    private enum class Page { START, KEY }

    private fun setPage(page: Page) {
        listOf(
            binding.pageStart to Page.START,
            binding.pageKey to Page.KEY,
        ).forEach { (view, withPage) ->
            view.visibility = if (page == withPage) View.VISIBLE else View.GONE
        }
    }
}