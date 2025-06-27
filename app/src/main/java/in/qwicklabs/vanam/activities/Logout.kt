package `in`.qwicklabs.vanam.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.google.firebase.auth.FirebaseAuth
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.databinding.ActivityLogoutBinding

class Logout : AppCompatActivity() {
    private lateinit var binding: ActivityLogoutBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLogoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars =
            true
        window.navigationBarColor = getColor(R.color.dashboardBg)

        // Handle Click Listeners
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.cancelButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.logoutButton.setOnClickListener {
            auth.signOut()

            getSharedPreferences("VanamPrefs", MODE_PRIVATE).edit { clear() }
            cacheDir.deleteRecursively()

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

        }
    }
}