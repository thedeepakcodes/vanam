package `in`.qwicklabs.vanam

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import `in`.qwicklabs.vanam.databinding.ActivityPhoneSupportBinding

class PhoneSupport : AppCompatActivity() {
    private lateinit var binding: ActivityPhoneSupportBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPhoneSupportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars =
            true
        window.navigationBarColor = getColor(R.color.dashboardBg)

        // Click Listeners
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.callGeneral.setOnClickListener {
            dialNumber("+918001234567")
        }

        binding.callTech.setOnClickListener {
            dialNumber("+918007654321")
        }

        binding.callBilling.setOnClickListener {
            dialNumber("+918002468100")
        }

    }

    private fun dialNumber(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phone")
        startActivity(intent)
    }
}