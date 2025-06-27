package `in`.qwicklabs.vanam.activities

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.databinding.ActivityEditProfileBinding
import `in`.qwicklabs.vanam.model.User
import `in`.qwicklabs.vanam.repository.FirebaseRepository
import `in`.qwicklabs.vanam.repository.UserRepository
import `in`.qwicklabs.vanam.utils.Loader
import kotlinx.coroutines.launch

class EditProfile : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var loader: Loader
    private var currentUser: User? = null

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
        lifecycleScope.launch {
            try {
                currentUser = UserRepository.getUser()

                if (currentUser !== null) {
                    binding.etFullName.setText(currentUser?.name)
                    binding.username.setText(currentUser?.username)
                    binding.etBio.setText(currentUser?.bio)
                    binding.phoneNumber.setText(currentUser?.phoneNumber)

                    val imageUrl = currentUser?.photoUrl
                    Glide.with(this@EditProfile)
                        .load(imageUrl)
                        .placeholder(R.drawable.circular_loader)
                        .error(R.drawable.profile_sample)
                        .circleCrop()
                        .into(binding.profileImage)
                } else {
                    Toast.makeText(this@EditProfile, "User profile not found.", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditProfile, "Error loading profile data.", Toast.LENGTH_SHORT)
                    .show()
            }
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
        val phoneNo = binding.phoneNumber.text.toString().trim()

        if (!validateInputs(fullName, username, bio, phoneNo)) return

        val userId = FirebaseRepository.getCurrentUserId()

        if (userId == "") {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        loader.title.text = "Updating Profile..."
        loader.dialog.show()

        if (isImageSelected && profileUri != null) {
            uploadProfileImage(profileUri!!) { imageUrl ->
                saveUserData(userId, fullName, username, bio, phoneNo, imageUrl)
            }
        } else {
            saveUserData(userId, fullName, username, bio, phoneNo, null)
        }
    }

    private fun uploadProfileImage(uri: Uri, onComplete: (String?) -> Unit) {
        val profileImageRef = FirebaseRepository.getUserProfileImageRef()

        profileImageRef.putFile(uri)
            .addOnSuccessListener {
                profileImageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onComplete(downloadUri.toString())
                }.addOnFailureListener { e ->
                    loader.dialog.dismiss()
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
        phoneNo: String,
        imageUrl: String?
    ) {
        currentUser?.name = fullName
        currentUser?.username = username
        currentUser?.bio = bio
        currentUser?.phoneNumber = phoneNo

        if (!imageUrl.isNullOrEmpty()) {
            currentUser?.photoUrl = imageUrl
        }

        lifecycleScope.launch {
            try {
                currentUser?.let { UserRepository.updateUser(it) }
                loader.dialog.dismiss()
                Toast.makeText(
                    this@EditProfile,
                    "Profile updated successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } catch (e: Exception) {
                loader.dialog.dismiss()
                Toast.makeText(this@EditProfile, "Failed to save profile.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Input validation method
    private fun validateInputs(
        fullName: String,
        username: String,
        bio: String,
        phoneNo: String
    ): Boolean {
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

        if (phoneNo.length != 10) {
            Toast.makeText(this, "Phone number must be 10 digits.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (phoneNo.startsWith("0")) {
            Toast.makeText(this, "Phone number cannot start with 0.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!phoneNo.matches(Regex("\\d+"))) {
            Toast.makeText(this, "Phone number must contain only digits.", Toast.LENGTH_SHORT)
                .show()
            return false
        }

        return true
    }
}
