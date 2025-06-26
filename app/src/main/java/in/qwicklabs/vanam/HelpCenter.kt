package `in`.qwicklabs.vanam

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import `in`.qwicklabs.vanam.databinding.ActivityHelpCenterBinding

class HelpCenter : AppCompatActivity() {
    private lateinit var binding: ActivityHelpCenterBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHelpCenterBinding.inflate(layoutInflater)
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

        binding.emailSupport.setOnClickListener {
            startActivity(Intent(this, EmailSupport::class.java))
        }

        binding.liveChatSupport.setOnClickListener {
            startActivity(Intent(this, ChatSupport::class.java))
        }

        binding.phoneSupport.setOnClickListener {
            startActivity(Intent(this, PhoneSupport::class.java))
        }
    }
}