package `in`.qwicklabs.vanam

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import `in`.qwicklabs.vanam.databinding.ActivitySettingsBinding

class Settings : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val userCollection by lazy {
        firestore.collection("Vanam").document("Users").collection("Profile")
    }

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

        loadProfileData()

        // Hnalde Click Listeners
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.changePass.setOnClickListener {
            val intent = Intent(this, ChangePass::class.java)
            startActivity(intent)
        }

        binding.linkedAccount.setOnClickListener {
            startActivity(Intent(this, LinkedAccounts::class.java))
        }

        binding.privacySettings.setOnClickListener {
            startActivity(Intent(this, PrivacySettings::class.java))
        }

        binding.deleteAccount.setOnClickListener {
            startActivity(Intent(this, DeleteAccount::class.java))
        }

        binding.helpCenter.setOnClickListener {
            startActivity(Intent(this, HelpCenter::class.java))
        }

        binding.termsAndPolicy.setOnClickListener {
            startActivity(Intent(this, TermsAndPrivacy::class.java))
        }

        binding.logOut.setOnClickListener {
            startActivity(Intent(this, Logout::class.java))
        }

    }

    private fun loadProfileData() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            userCollection.document(userId).get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val userName = documentSnapshot.getString("name")
                    val userEmail = currentUser.email
                    val profileImageUrl =
                        documentSnapshot.getString("profileImage") ?: currentUser.photoUrl

                    binding.userName.text = userName ?: "N/A"
                    binding.userEmail.text = userEmail ?: "N/A"

                    Glide.with(this).load(profileImageUrl).placeholder(R.drawable.circular_loader)
                        .error(R.drawable.profile_sample).circleCrop().into(binding.profileImage)
                } else {
                    Toast.makeText(this, "Profile data not found.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(
                    this, "Failed to load profile data: ${e.message}", Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }
}