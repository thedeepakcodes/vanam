package `in`.qwicklabs.vanam.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.databinding.ActivitySettingsBinding
import `in`.qwicklabs.vanam.viewModel.UserViewModel
import kotlinx.coroutines.launch

class Settings : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars =
            true

        window.navigationBarColor = getColor(R.color.dashboardBg)

        observeUserData()

        binding.apply {
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            changePass.setOnClickListener {
                startActivity(Intent(this@Settings, ChangePass::class.java))

            }

            linkedAccount.setOnClickListener {
                startActivity(Intent(this@Settings, LinkedAccounts::class.java))
            }

            privacySettings.setOnClickListener {
                startActivity(Intent(this@Settings, PrivacySettings::class.java))
            }

            deleteAccount.setOnClickListener {
                startActivity(Intent(this@Settings, DeleteAccount::class.java))
            }

            helpCenter.setOnClickListener {
                startActivity(Intent(this@Settings, HelpCenter::class.java))
            }

            termsAndPolicy.setOnClickListener {
                startActivity(Intent(this@Settings, TermsAndPrivacy::class.java))
            }

            logOut.setOnClickListener {
                startActivity(Intent(this@Settings, Logout::class.java))
            }

            editProfile.setOnClickListener {
                startActivity(Intent(this@Settings, EditProfile::class.java))
            }

            emailNotificationSwitch.setOnCheckedChangeListener { _, isChecked ->
                updateNotificationSettings()
                val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
                Toast.makeText(this@Settings, "Email $message", Toast.LENGTH_SHORT).show()
            }

            pushNotificationSwitch.setOnCheckedChangeListener { _, isChecked ->
                updateNotificationSettings()
                val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
                Toast.makeText(this@Settings, "Push $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeUserData() {
        try {
            userViewModel.user.observe(this) { user ->
                if (user !== null) {
                    binding.userName.text = user.name ?: "N/A"
                    binding.userEmail.text = user.email ?: "N/A"

                    val profileImageUrl = user.photoUrl

                    Glide.with(this).load(profileImageUrl).placeholder(R.drawable.circular_loader)
                        .error(R.drawable.profile_sample).circleCrop().into(binding.profileImage)
                } else {
                    Toast.makeText(this, "Profile data not found.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                this, "Failed to load profile data: ${e.message}", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateNotificationSettings() {
        val emailNotificationsEnabled = binding.emailNotificationSwitch.isChecked
        val pushNotificationsEnabled = binding.pushNotificationSwitch.isChecked

        lifecycleScope.launch {
            val user = userViewModel.user.value

            if (user != null) {
                user.isEmailNotificationsEnabled = emailNotificationsEnabled
                user.isPushNotificationsEnabled = pushNotificationsEnabled

                try {
                    userViewModel.updateUser(user)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@Settings,
                        "Failed to update notification settings: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}