package `in`.qwicklabs.vanam.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import `in`.qwicklabs.vanam.Dashboard
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.databinding.ActivityBasicProfileBinding
import `in`.qwicklabs.vanam.utils.Loader
import kotlin.random.Random

class BasicActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBasicProfileBinding
    private lateinit var loader: Loader

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var imageUri: Uri? = null
    private var isImageSelected = false
    private var existingProfileData: Map<String, Any>? = null

    private val storageRef by lazy { FirebaseStorage.getInstance().reference }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val userCollection by lazy {
        firestore.collection("Vanam").document("Users").collection("Profile")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityBasicProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loader = Loader(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, insets.top, view.paddingRight, insets.bottom)
            windowInsets
        }

        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true


        // Move to Dashboard if Profile Setup is Complete
        if (getSharedPreferences("VanamPrefs", MODE_PRIVATE).getBoolean(
                "isProfileComplete",
                false
            )
        ) {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
            return
        }

        // Move to Interest Activity if Basic Profile is Complete
        if (getSharedPreferences("VanamPrefs", MODE_PRIVATE).getBoolean(
                "isBasicProfileComplete",
                false
            )
        ) {
            startActivity(Intent(this, InterestActivity::class.java))
            finish()
            return
        }


        setupImagePicker()
        setupPlantingGoalControls()
        setupSaveContinueButton()
        setupCountrySpinner()
        fetchAndAutofillProfile()
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
            val currentGoal = binding.plantingGoal.text.toString().toIntOrNull() ?: 0
            binding.plantingGoal.text = (currentGoal + 5).toString()
        }

        binding.minusPlantingGoal.setOnClickListener {
            val currentGoal = binding.plantingGoal.text.toString().toIntOrNull() ?: 1
            if (currentGoal > 1) {
                binding.plantingGoal.text = (currentGoal - 1).toString()
            }
        }
    }

    private fun setupSaveContinueButton() {
        binding.saveContinue.setOnClickListener {
            val fullName = binding.fullName.text.toString().trim()
            val bio = binding.bioText.text.toString().trim()
            val country = binding.countrySpinner.selectedItem.toString()
            val plantingGoal = binding.plantingGoal.text.toString().toIntOrNull() ?: 0

            // Input Validation
            if (fullName.isEmpty() || bio.isEmpty() || binding.countrySpinner.selectedItemPosition <= 0) {
                Toast.makeText(this, "Please fill in all the required fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (!fullName.all { it.isLetter() || it.isWhitespace() }) {
                Toast.makeText(this, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (fullName.length < 3 || fullName.length > 50) {
                Toast.makeText(this, "Name must be between 3 and 50 characters", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (bio.length > 500) {
                Toast.makeText(this, "Bio cannot be more than 500 characters", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Save Profile Data
            val changedFields = hashMapOf<String, Any>()
            val old = existingProfileData ?: emptyMap()

            if (old["name"] != fullName) changedFields["name"] = fullName
            if (old["bio"] != bio) changedFields["bio"] = bio
            if (old["country"] != country) changedFields["country"] = country
            if ((old["plantingGoal"] as? Long)?.toInt() != plantingGoal) changedFields["plantingGoal"] =
                plantingGoal

            val email = auth.currentUser?.email
            val username = email?.split("@")?.firstOrNull() ?: generateUsername()

            if (old["username"] == null) changedFields["username"] = username

            if (isImageSelected) {
                loader.title.text = "Uploading..."
                loader.message.text = "Uploading new profile image..."
                loader.dialog.show()
                uploadProfileImageWithDiff(changedFields)
            } else {
                if (changedFields.isNotEmpty()) {
                    saveUserProfileDiff(changedFields)
                } else {
                    getSharedPreferences("VanamPrefs", MODE_PRIVATE).edit {
                        putBoolean("isBasicProfileComplete", true)
                    }

                    startActivity(Intent(this, InterestActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun uploadProfileImageWithDiff(changedFields: HashMap<String, Any>) {
        val uid = auth.uid ?: return

        imageUri?.let { uri ->
            val imageRef = storageRef.child("Vanam/UserProfile/$uid")
            imageRef.putFile(uri).addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    changedFields["profileImage"] = downloadUrl.toString()
                    saveUserProfileDiff(changedFields)
                }
            }.addOnFailureListener {
                loader.dialog.dismiss()
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserProfileDiff(changes: HashMap<String, Any>) {
        val uid = auth.uid ?: return

        userCollection.document(uid).set(changes, SetOptions.merge()).addOnSuccessListener {
            getSharedPreferences("VanamPrefs", MODE_PRIVATE).edit {
                putBoolean("isBasicProfileComplete", true)
            }

            loader.dialog.dismiss()
            startActivity(Intent(this, InterestActivity::class.java))
            finish()
        }.addOnFailureListener { e ->
            loader.dialog.dismiss()
            Toast.makeText(
                this,
                "Failed to update profile: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupCountrySpinner() {
        val countries = resources.getStringArray(R.array.country_list)
        val adapter = ArrayAdapter(this, R.layout.spinner_item, countries)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.countrySpinner.adapter = adapter
    }

    private fun fetchAndAutofillProfile() {
        val uid = auth.uid ?: return

        loader.title.text = "Loading..."
        loader.message.text = "Fetching your profile data..."
        loader.dialog.show()

        userCollection.document(uid).get().addOnSuccessListener { document ->
            existingProfileData = document.data

            val user = auth.currentUser
            val name = document.getString("name") ?: user?.displayName.toString()
            val profileImage = document.getString("profileImage") ?: user?.photoUrl.toString()

            binding.fullName.setText(name)
            binding.bioText.setText(document.getString("bio"))

            Glide.with(this)
                .load(profileImage)
                .placeholder(R.drawable.profile_sample)
                .into(binding.profileImageView)

            isImageSelected = false
            binding.imageChooserIcon.visibility = View.GONE

            document.getString("country")?.let { country ->
                val countries = resources.getStringArray(R.array.country_list)
                val index = countries.indexOf(country)
                if (index >= 0) {
                    binding.countrySpinner.setSelection(index)
                }
            }

            val plantingGoal = document.getLong("plantingGoal")?.toInt() ?: 1
            binding.plantingGoal.text = plantingGoal.toString()

            loader.dialog.dismiss()
        }.addOnFailureListener {
            loader.dialog.dismiss()
            Toast.makeText(this, "Failed to fetch profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateUsername(): String {
        val length = Random.nextInt(6, 11)
        val letters = ('a'..'z')
        return (1..length)
            .map { letters.random() }
            .joinToString("")
    }
}
