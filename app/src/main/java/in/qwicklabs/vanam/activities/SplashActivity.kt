package `in`.qwicklabs.vanam.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import `in`.qwicklabs.vanam.databinding.ActivitySplashBinding
import `in`.qwicklabs.vanam.profile.BasicActivity
import `in`.qwicklabs.vanam.repository.FirebaseRepository
import `in`.qwicklabs.vanam.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val sharedPrefs by lazy { getSharedPreferences("VanamPrefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            checkUserStatus()
        }
    }

    private fun checkUserStatus() {
        val currentUser = FirebaseRepository.getAuthInstance().currentUser

        if (currentUser == null) {
            // Not logged in -> Go to welcome screen
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        } else {
            if (sharedPrefs.getBoolean("isProfileSetupComplete", false)) {
                startActivity(Intent(this@SplashActivity, Dashboard::class.java))
                finish()
            } else {
                lifecycleScope.launch {
                    val user = UserRepository.getUser()

                    if (user?.isProfileSetupComplete == true) {
                        sharedPrefs.edit { putBoolean("isProfileSetupComplete", true) }
                        startActivity(Intent(this@SplashActivity, Dashboard::class.java))
                        finish()
                    } else {
                        startActivity(Intent(this@SplashActivity, BasicActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}
