package es.gde.unizar.puf

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import es.gde.unizar.puf.databinding.ActivityMainBinding

private const val EXPECTED = "110101110101111110101000000010011000101000111010011100111000000010011000110000110101110001001011000000000000"

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val processor = KeyProcessor(this)

        binding.btnGet.setOnClickListener {
            binding.switcher.displayedChild = 1
            binding.txtKey.text = processor.main(R.raw.example, R.raw.example2, R.raw.example3)
            Log.d("GOT", binding.txtKey.text.toString())
            Log.d("EXPECTED", EXPECTED)
            if (binding.txtKey.text.toString() != EXPECTED) Toast.makeText(this, "Different!", Toast.LENGTH_SHORT).show()
        }
        binding.btnReset.setOnClickListener { binding.switcher.displayedChild = 0 }
    }
}