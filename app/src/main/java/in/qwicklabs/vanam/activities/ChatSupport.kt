package `in`.qwicklabs.vanam.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.databinding.ActivityChatSupportBinding

class ChatSupport : AppCompatActivity() {
    private lateinit var binding: ActivityChatSupportBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityChatSupportBinding.inflate(layoutInflater)
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
    }
}