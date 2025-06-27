package `in`.qwicklabs.vanam.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.databinding.ActivityDeleteAccountBinding
import `in`.qwicklabs.vanam.model.User
import `in`.qwicklabs.vanam.repository.FirebaseRepository
import `in`.qwicklabs.vanam.repository.UserRepository
import `in`.qwicklabs.vanam.utils.PasswordHasher
import kotlinx.coroutines.launch

class DeleteAccount : AppCompatActivity() {

    private lateinit var binding: ActivityDeleteAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDeleteAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        window.navigationBarColor = getColor(R.color.dashboardBg)

        // Click Listeners
        binding.apply {
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            cancelButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            deleteAccountButton.setOnClickListener {
                showDeleteConfirmationDialog()
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        val password = binding.password.text.toString().trim()
        val isConsentChecked = binding.deleteConsent.isChecked

        if (password.isEmpty()) {
            binding.password.error = "Please enter your password"
            return
        }

        if (!isConsentChecked) {
            Toast.makeText(this, "Please check the consent checkbox", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Account Deletion")
            .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                dialog.dismiss()
                deleteAccount(password)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteAccount(password: String) {
        val userId = FirebaseRepository.getCurrentUserId()

        if (userId == "") {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            resetDeleteButtonState()
            return
        }

        binding.deleteAccountButton.isEnabled = false
        binding.deleteAccountButton.text = "Deleting..."

        lifecycleScope.launch {
            val user: User? = UserRepository.getUser()

            if (user == null) {
                resetDeleteButtonState()
                proceedToLogin("Failed to fetch user data for account deletion.")
                return@launch
            }

            val storedPassword = user.password

            if (storedPassword.isNullOrEmpty()) {
                resetDeleteButtonState()
                Toast.makeText(this@DeleteAccount, "Password not set.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (PasswordHasher.checkPassword(password, storedPassword)) {
                try {
                    UserRepository.deleteUser()
                    proceedToLogin("Account deleted successfully!")
                } catch (e: Exception) {
                    resetDeleteButtonState()
                    Toast.makeText(
                        this@DeleteAccount,
                        "An error occurred during account deletion. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
            } else {
                resetDeleteButtonState()
                Toast.makeText(this@DeleteAccount, "Incorrect Password.", Toast.LENGTH_SHORT).show()
                return@launch
            }
        }
    }

    private fun resetDeleteButtonState() {
        binding.deleteAccountButton.isEnabled = true
        binding.deleteAccountButton.text = "Delete Account"
    }

    private fun proceedToLogin(message: String) {
        FirebaseRepository.getAuthInstance().signOut()
        getSharedPreferences("VanamPrefs", MODE_PRIVATE).edit { clear() }
        cacheDir.deleteRecursively()

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}