package `in`.qwicklabs.vanam

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import `in`.qwicklabs.vanam.databinding.ActivityEditProfileBinding
import `in`.qwicklabs.vanam.utils.Loader

class EditProfile : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var loader: Loader

    private val storageRef by lazy { FirebaseStorage.getInstance().reference }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val userCollection by lazy {
        firestore.collection("Vanam").document("Users").collection("Profile")
    }

    private var profileUri: Uri? = null
    private var isImageSelected = false

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                binding.profileImage.setImageURI(it)
                profileUri = it
                isImageSelected = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loader = Loader(this)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars =
            true
        window.navigationBarColor = getColor(R.color.dashboardBg)

        loadUserData()
        setupClickListeners()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        userCollection.document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.etFullName.setText(document.getString("name"))
                    binding.username.setText(document.getString("username"))
                    binding.etBio.setText(document.getString("bio"))

                    val imageUrl =
                        document.getString("profileImage") ?: auth.currentUser?.photoUrl.toString()
                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.circular_loader)
                        .error(R.drawable.profile_sample)
                        .circleCrop()
                        .into(binding.profileImage)
                } else {
                    Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("EditProfile", "Failed to load user data: ${e.message}", e)
                Toast.makeText(this, "Error loading profile data.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupClickListeners() {
        binding.imgBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.tvChangePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            handleSave()
        }
    }

    private fun handleSave() {
        val fullName = binding.etFullName.text.toString().trim()
        val username = binding.username.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()

        if (!validateInputs(fullName, username, bio)) return

        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        loader.title.text = "Updating Profile..."
        loader.dialog.show()

        if (isImageSelected && profileUri != null) {
            uploadProfileImage(profileUri!!) { imageUrl ->
                saveUserData(userId, fullName, username, bio, imageUrl)
            }
        } else {
            saveUserData(userId, fullName, username, bio, null)
        }
    }

    private fun uploadProfileImage(uri: Uri, onComplete: (String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        val profileImageRef = storageRef.child("Vanam/UserProfile/$userId")

        profileImageRef.putFile(uri)
            .addOnSuccessListener {
                profileImageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onComplete(downloadUri.toString())
                }.addOnFailureListener { e ->
                    loader.dialog.dismiss()
                    Log.e("EditProfile", "Failed to get download URL: ${e.message}", e)
                    Toast.makeText(this, "Failed to get image URL.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                loader.dialog.dismiss()
                Log.e("EditProfile", "Image upload failed: ${e.message}", e)
                Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserData(
        userId: String,
        fullName: String,
        username: String,
        bio: String,
        imageUrl: String?
    ) {
        val userData = mutableMapOf(
            "name" to fullName,
            "username" to username,
            "bio" to bio
        )

        if (!imageUrl.isNullOrEmpty()) {
            userData["profileImage"] = imageUrl
        }

        userCollection.document(userId)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                loader.dialog.dismiss()
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

                val resultIntent = Intent().apply {
                    putExtra("name", fullName)
                    putExtra("username", username)
                    putExtra("bio", bio)
                    if (!imageUrl.isNullOrEmpty()) {
                        putExtra("profileImage", imageUrl)
                    }
                }

                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
            .addOnFailureListener { e ->
                loader.dialog.dismiss()
                Log.e("EditProfile", "Failed to update user data: ${e.message}", e)
                Toast.makeText(this, "Failed to save profile.", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔍 Input validation method
    private fun validateInputs(fullName: String, username: String, bio: String): Boolean {
        if (fullName.length < 3) {
            Toast.makeText(this, "Name must be at least 3 characters.", Toast.LENGTH_SHORT)
                .show()
            return false
        }

        if (fullName.length > 50) {
            Toast.makeText(this, "Name cannot exceed 50 characters.", Toast.LENGTH_SHORT)
                .show()
            return false
        }

        val usernamePattern = "^[a-zA-Z0-9_]{3,20}$".toRegex()
        if (!username.matches(usernamePattern)) {
            Toast.makeText(
                this,
                "Username must be 3–20 characters and contain only letters, numbers, or underscores.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        if (bio.length > 150) {
            Toast.makeText(this, "Bio cannot exceed 150 characters.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}
