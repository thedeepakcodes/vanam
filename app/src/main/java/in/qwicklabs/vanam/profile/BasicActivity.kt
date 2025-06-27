package `in`.qwicklabs.vanam.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.activities.Dashboard
import `in`.qwicklabs.vanam.databinding.ActivityBasicProfileBinding
import `in`.qwicklabs.vanam.model.User
import `in`.qwicklabs.vanam.repository.FirebaseRepository
import `in`.qwicklabs.vanam.repository.UserRepository
import `in`.qwicklabs.vanam.utils.Loader
import kotlinx.coroutines.launch
import kotlin.random.Random

class BasicActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBasicProfileBinding
    private lateinit var loader: Loader
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    private var imageUri: Uri? = null
    private var isImageSelected = false
    private var existingProfileData: User? = null

    private val auth = FirebaseRepository.getAuthInstance()
    private val sharedPrefs by lazy { getSharedPreferences("VanamPrefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBasicProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loader = Loader(this)
        applyWindowInsets()
        setStatusBarAppearance()

        if (sharedPrefs.getBoolean("isProfileSetupComplete", false)) {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }

        if (sharedPrefs.getBoolean("ProfileSetup_1", false)) {
            startActivity(Intent(this, InterestActivity::class.java))
            finish()
        }

        setupImagePicker()
        setupPlantingGoalControls()
        setupSaveContinueButton()
        setupCountrySpinner()
        fetchAndAutofillProfile()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, insets.top, view.paddingRight, insets.bottom)
            windowInsets
        }
    }

    private fun setStatusBarAppearance() {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            true
    }

    private fun setupImagePicker() {
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    imageUri = it
                    isImageSelected = true
                    binding.profileImageView.setImageURI(it)
                    binding.imageChooserIcon.visibility = View.GONE
                }
            }

        binding.profilePictureFrame.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun setupPlantingGoalControls() {
        binding.plusPlantingGoal.setOnClickListener {
            updatePlantingGoal(5)
        }

        binding.minusPlantingGoal.setOnClickListener {
            updatePlantingGoal(-1)
        }
    }

    private fun updatePlantingGoal(change: Int) {
        val currentGoal = binding.plantingGoal.text.toString().toIntOrNull() ?: 0
        val newGoal = if (currentGoal + change > 0) currentGoal + change else 1
        binding.plantingGoal.text = newGoal.toString()
    }

    private fun setupSaveContinueButton() {
        binding.saveContinue.setOnClickListener {
            if (validateInputs()) {
                saveProfileData()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val fullName = binding.fullName.text.toString().trim()
        val bio = binding.bioText.text.toString().trim()
        val countrySelection = binding.countrySpinner.selectedItemPosition

        return when {
            fullName.isEmpty() || bio.isEmpty() || countrySelection <= 0 -> {
                showToast("Please fill in all the required fields")
                false
            }

            !fullName.all { it.isLetter() || it.isWhitespace() } -> {
                showToast("Please enter a valid name")
                false
            }

            fullName.length < 3 || fullName.length > 50 -> {
                showToast("Name must be between 3 and 50 characters")
                false
            }

            bio.length > 500 -> {
                showToast("Bio cannot be more than 500 characters")
                false
            }

            else -> true
        }
    }

    private fun saveProfileData() {
        val fullName = binding.fullName.text.toString().trim()
        val bio = binding.bioText.text.toString().trim()
        val country = binding.countrySpinner.selectedItem.toString()
        val plantingGoal = binding.plantingGoal.text.toString().toIntOrNull() ?: 0

        val updatedUser =
            existingProfileData?.copy() ?: auth.currentUser?.email?.substringBefore("@")?.let {
                User(
                    it, auth.currentUser?.email ?: "", ""
                )
            }

        if (updatedUser?.name != fullName) updatedUser?.name = fullName
        if (updatedUser?.bio != bio) updatedUser?.bio = bio
        if (updatedUser?.country != country) updatedUser?.country = country
        if (updatedUser?.plantingGoal != plantingGoal) updatedUser?.plantingGoal = plantingGoal
        if (updatedUser?.joinedAt == null) updatedUser?.joinedAt = System.currentTimeMillis()
        if (updatedUser?.photoUrl == null) {
            updatedUser?.photoUrl = auth.currentUser?.photoUrl.toString()
        }
        val username = auth.currentUser?.email?.substringBefore("@") ?: generateUsername()
        if (updatedUser?.username.isNullOrEmpty()) updatedUser?.username = username

        if (isImageSelected) {
            if (updatedUser != null) {
                uploadProfileImage(updatedUser)
            }
        } else {
            if (updatedUser != existingProfileData || existingProfileData == null) {
                if (updatedUser != null) {
                    saveUserProfile(updatedUser)
                }
            } else {
                navigateToInterestActivity()
            }
        }
    }

    private fun uploadProfileImage(user: User) {
        auth.uid ?: run {
            showToast("User not logged in")
            return
        }

        imageUri?.let { uri ->
            loader.title.text = "Uploading..."
            loader.message.text = "Uploading new profile image..."
            loader.dialog.show()

            val imageRef = FirebaseRepository.getUserProfileImageRef()
            imageRef.putFile(uri).addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    user.photoUrl = downloadUrl.toString()
                    saveUserProfile(user)
                }
            }.addOnFailureListener { e ->
                loader.dialog.dismiss()
                showToast("Failed to upload image: ${e.localizedMessage}")
            }
        } ?: run {
            showToast("No image selected to upload.")
        }
    }

    private fun saveUserProfile(user: User) {
        auth.uid ?: run {
            showToast("User not logged in")
            return
        }

        lifecycleScope.launch {
            try {
                UserRepository.updateUser(user)
                navigateToInterestActivity()
            } catch (e: Exception) {
                loader.dialog.dismiss()
                showToast("Failed to update profile: ${e.localizedMessage}")
            }
        }
    }

    private fun navigateToInterestActivity() {
        sharedPrefs.edit { putBoolean("ProfileSetup_1", true) }
        loader.dialog.dismiss()
        startActivity(Intent(this, InterestActivity::class.java))
        finish()
    }

    private fun setupCountrySpinner() {
        val countries = resources.getStringArray(R.array.country_list)
        val adapter = ArrayAdapter(this, R.layout.spinner_item, countries)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.countrySpinner.adapter = adapter
    }

    private fun fetchAndAutofillProfile() {
        loader.title.text = "Loading..."
        loader.message.text = "Fetching your profile data..."
        loader.dialog.show()

        lifecycleScope.launch {
            try {
                existingProfileData = UserRepository.getUser()

                val user = auth.currentUser

                val name = existingProfileData?.name ?: user?.displayName.orEmpty()
                val profileImage = existingProfileData?.photoUrl ?: user?.photoUrl.toString()

                binding.fullName.setText(name)
                binding.bioText.setText(existingProfileData?.bio)

                Glide.with(this@BasicActivity).load(profileImage)
                    .placeholder(R.drawable.profile_sample)
                    .into(binding.profileImageView)

                isImageSelected = false
                binding.imageChooserIcon.visibility = View.GONE

                existingProfileData?.country?.let { country ->
                    val countries = resources.getStringArray(R.array.country_list)
                    val index = countries.indexOf(country)
                    if (index >= 0) {
                        binding.countrySpinner.setSelection(index)
                    }
                }

                val plantingGoal = existingProfileData?.plantingGoal ?: 1
                binding.plantingGoal.text = plantingGoal.toString()

                loader.dialog.dismiss()
            } catch (e: Exception) {
                loader.dialog.dismiss()
                showToast("Failed to fetch profile data: ${e.localizedMessage}")
            }
        }
    }

    private fun generateUsername(): String {
        val length = Random.nextInt(6, 11)
        val letters = ('a'..'z')
        return (1..length).map { letters.random() }.joinToString("")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}