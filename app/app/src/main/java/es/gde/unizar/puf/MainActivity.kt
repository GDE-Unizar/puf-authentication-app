package es.gde.unizar.puf

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import es.gde.unizar.puf.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGet.setOnClickListener {
            binding.switcher.displayedChild = 1
        }
        binding.btnReset.setOnClickListener { binding.switcher.displayedChild = 0 }
    }
}