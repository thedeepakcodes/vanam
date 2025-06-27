package `in`.qwicklabs.vanam.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.databinding.ActivityLinkedAccountsBinding

class LinkedAccounts : AppCompatActivity() {
    private lateinit var binding: ActivityLinkedAccountsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLinkedAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars =
            true

        window.navigationBarColor = getColor(R.color.dashboardBg)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.googleActionBtn.setOnClickListener {
            Toast.makeText(this, "You cannot disconnect Google Auth Provider.", Toast.LENGTH_SHORT)
                .show()
        }
    }
}