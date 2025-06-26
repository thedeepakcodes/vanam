package `in`.qwicklabs.vanam

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import `in`.qwicklabs.vanam.databinding.ActivityDeleteAccountBinding
import `in`.qwicklabs.vanam.utils.Loader

class DeleteAccount : AppCompatActivity() {

    private lateinit var binding: ActivityDeleteAccountBinding
    private lateinit var loader: Loader

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val userCollection by lazy {
        firestore.collection("Vanam").document("Users").collection("Profile")
    }

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

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.cancelButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.deleteAccountButton.setOnClickListener {
            attemptDeleteAccount()
        }
    }

    private fun attemptDeleteAccount() {
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

        loader = Loader(this).apply {
            title.text = "Deleting Account..."
            message.text = "Please wait while we delete your account."
        }

        deleteAccount(password)
    }

    private fun deleteAccount(password: String) {
        val user = auth.currentUser
        val userId = user?.uid ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        loader.dialog.show()

        userCollection.document(userId).get().addOnSuccessListener { document ->
            val storedPassword = document.getString("password").orEmpty()

            if (storedPassword.isEmpty() || storedPassword == "null") {
                loader.dialog.dismiss()
                Toast.makeText(this, "Password not set.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            if (storedPassword != password) {
                loader.dialog.dismiss()
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            userCollection.document(userId).delete().addOnSuccessListener {

                user.delete().addOnSuccessListener {
                    proceedToLogin("Your account has been deleted permanently.")
                }.addOnFailureListener {
                    proceedToLogin("Your account has been deleted.")
                }

            }.addOnFailureListener {
                loader.dialog.dismiss()
                Toast.makeText(this, "Failed to delete your account.", Toast.LENGTH_SHORT)
                    .show()
            }

        }.addOnFailureListener {
            loader.dialog.dismiss()
            Toast.makeText(this, "Failed to verify that it's you.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun proceedToLogin(message: String) {
        loader.dialog.dismiss()

        auth.signOut()
        getSharedPreferences("VanamPrefs", MODE_PRIVATE).edit { clear() }
        cacheDir.deleteRecursively()

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
