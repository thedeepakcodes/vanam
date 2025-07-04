package `in`.qwicklabs.vanam.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.databinding.ActivityChangePassBinding
import `in`.qwicklabs.vanam.model.User
import `in`.qwicklabs.vanam.repository.FirebaseRepository
import `in`.qwicklabs.vanam.repository.UserRepository
import `in`.qwicklabs.vanam.utils.PasswordHasher
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class ChangePass : AppCompatActivity() {

    private lateinit var binding: ActivityChangePassBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityChangePassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        window.navigationBarColor = getColor(R.color.dashboardBg)
    }

    private fun setupListeners() {
        binding.apply {
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            btnUpdatePassword.setOnClickListener {
                attemptPasswordUpdate()
            }
        }
    }

    private fun attemptPasswordUpdate() {
        val currentPassword = binding.etCurrentPassword.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmNewPassword = binding.etConfirmNewPassword.text.toString().trim()

        clearInputErrors()

        when {
            currentPassword.isEmpty() -> {
                binding.etCurrentPassword.error = ERROR_EMPTY_CURRENT
                showToast("Please enter your current password.")
                return
            }

            newPassword.isEmpty() -> {
                binding.etNewPassword.error = ERROR_EMPTY_NEW
                showToast("Please enter your new password.")
                return
            }

            confirmNewPassword.isEmpty() -> {
                binding.etConfirmNewPassword.error = ERROR_EMPTY_CONFIRM
                showToast("Please confirm your new password.")
                return
            }

            !validateNewPassword(newPassword, confirmNewPassword) -> {
                showToast("Please fix the new password errors.")
                return
            }

            else -> {
                updatePassword(currentPassword, newPassword)
            }
        }
    }

    private fun clearInputErrors() {
        binding.apply {
            etCurrentPassword.error = null
            etNewPassword.error = null
            etConfirmNewPassword.error = null
        }
    }

    private fun validateNewPassword(newPassword: String, confirmNewPassword: String): Boolean {
        var isValid = true

        if (newPassword.length < 6) {
            binding.etNewPassword.error = ERROR_MIN_LENGTH
            isValid = false
        }

        if (!Pattern.compile(".*\\d.*").matcher(newPassword).matches()) {
            binding.etNewPassword.error = ERROR_MISSING_NUMBER
            isValid = false
        }

        if (!Pattern.compile(".*[A-Z].*").matcher(newPassword).matches()) {
            binding.etNewPassword.error = ERROR_MISSING_UPPERCASE
            isValid = false
        }

        if (!Pattern.compile(".*[a-z].*").matcher(newPassword).matches()) {
            binding.etNewPassword.error = ERROR_MISSING_LOWERCASE
            isValid = false
        }

        val specialCharPattern =
            Pattern.compile(".*[!@#\\\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")
        if (!specialCharPattern.matcher(newPassword).matches()) {
            binding.etNewPassword.error = ERROR_MISSING_SPECIAL
            isValid = false
        }

        if (newPassword != confirmNewPassword) {
            binding.etConfirmNewPassword.error = ERROR_PASSWORD_MISMATCH
            isValid = false
        }

        return isValid
    }

    private fun updatePassword(currentPassword: String, newPassword: String) {
        val userId = FirebaseRepository.getCurrentUserId()

        if (userId == "") {
            showToast("User not logged in.")
            return
        }

        val updateButton = binding.btnUpdatePassword
        val originalText = updateButton.text.toString()

        updateButton.isEnabled = false
        updateButton.text = "Updating..."

        lifecycleScope.launch {
            try {
                val user: User? = UserRepository.getUser()

                if (user == null) {
                    onPasswordUpdateFailure(
                        originalText,
                        "Failed to fetch user data for password update."
                    )
                    return@launch
                }

                val storedHashedPassword = user.password

                if (storedHashedPassword.isNullOrEmpty() && currentPassword == DEFAULT_PASSWORD) {
                    user.password = PasswordHasher.hashPassword(newPassword)
                } else if (!storedHashedPassword.isNullOrEmpty()) {
                    if (PasswordHasher.checkPassword(currentPassword, storedHashedPassword)) {
                        user.password = PasswordHasher.hashPassword(newPassword)
                    } else {
                        onPasswordUpdateFailure(originalText, "Incorrect current password.")
                        return@launch
                    }
                } else {
                    onPasswordUpdateFailure(
                        originalText,
                        "Password not set. Use default password."
                    )
                    return@launch
                }

                UserRepository.updateUser(user)
                onPasswordUpdateSuccess(originalText)

            } catch (e: Exception) {
                onPasswordUpdateFailure(
                    originalText,
                    "An error occurred during password update. Please try again."
                )
                return@launch
            }
        }
    }


    private fun onPasswordUpdateSuccess(originalButtonText: String) {
        binding.btnUpdatePassword.apply {
            isEnabled = true
            text = originalButtonText
        }
        showToast("Password updated successfully!")
        finish()
    }

    private fun onPasswordUpdateFailure(originalButtonText: String, errorMessage: String) {
        binding.btnUpdatePassword.apply {
            isEnabled = true
            text = originalButtonText
        }
        showToast("Error: $errorMessage")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        // Validation Errors
        private const val ERROR_EMPTY_CURRENT = "Current password cannot be empty"
        private const val ERROR_EMPTY_NEW = "New password cannot be empty"
        private const val ERROR_EMPTY_CONFIRM = "Confirm new password cannot be empty"
        private const val ERROR_MIN_LENGTH = "Min 6 characters"
        private const val ERROR_MISSING_NUMBER = "Must contain a number"
        private const val ERROR_MISSING_UPPERCASE = "Must contain an uppercase letter"
        private const val ERROR_MISSING_LOWERCASE = "Must contain a lowercase letter"
        private const val ERROR_MISSING_SPECIAL = "Must contain a special character"
        private const val ERROR_PASSWORD_MISMATCH = "Passwords do not match"

        private const val DEFAULT_PASSWORD = "12345678"
    }
}
