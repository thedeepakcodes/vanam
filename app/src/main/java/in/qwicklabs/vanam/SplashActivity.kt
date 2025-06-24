package `in`.qwicklabs.vanam

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.firebase.auth.FirebaseAuth
import `in`.qwicklabs.vanam.profile.BasicActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set the status bar color to Dark
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        setContentView(R.layout.activity_splash)

        // Delay for 1 second and then check if the user is logged in or not
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)

            val currentUser = auth.currentUser

            if (currentUser != null) {
                startActivity(Intent(this@SplashActivity, BasicActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, WelcomeActivity::class.java))
            }
            finish()
        }
    }
}