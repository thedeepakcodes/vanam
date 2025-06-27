package `in`.qwicklabs.vanam.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.databinding.ActivityEmailSupportBinding

class EmailSupport : AppCompatActivity() {
    private lateinit var binding: ActivityEmailSupportBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityEmailSupportBinding.inflate(layoutInflater)
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

        binding.emailGeneral.setOnClickListener {
            sendSupportEmail("support@vanam.com", "Support Request - General")
        }

        binding.emailBilling.setOnClickListener {
            sendSupportEmail("billing@vanam.com", "Support Request - Billing")
        }

        binding.emailTechnical.setOnClickListener {
            sendSupportEmail("tech@vanam.com", "Support Request - Technical")
        }

    }

    private fun sendSupportEmail(email: String, subject: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }

        try {
            startActivity(Intent.createChooser(intent, "Send email via..."))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No email app installed.", Toast.LENGTH_SHORT).show()
        }
    }
}