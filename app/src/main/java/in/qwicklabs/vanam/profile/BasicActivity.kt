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
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import `in`.qwicklabs.vanam.Dashboard
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.databinding.ActivityBasicProfileBinding
import `in`.qwicklabs.vanam.utils.Loader

class BasicActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBasicProfileBinding
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var imageUri: Uri? = null
    private var isImageSelected = false
    private val storageRef by lazy { FirebaseStorage.getInstance().reference }
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var loader: Loader
    private var existingProfileData: Map<String, Any>? = null
    private val userCollection by lazy {
        firestore.collection("Vanam").document("Users").collection("Profile")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (getSharedPreferences("VanamPrefs", MODE_PRIVATE).getBoolean(
                "isProfileComplete",
                false
            )
        ) {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
            return
        }

        if (getSharedPreferences("VanamPrefs", MODE_PRIVATE).getBoolean(
                "isBasicProfileComplete",
                false
            )
        ) {
            startActivity(Intent(this, InterestActivity::class.java))
            finish()
            return
        }

        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding = ActivityBasicProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loader = Loader(this)
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
            val userName = binding.username.text.toString().trim()
            val bio = binding.bioText.text.toString().trim()
            val country = binding.countrySpinner.selectedItem.toString()
            val plantingGoal = binding.plantingGoal.text.toString().toIntOrNull() ?: 0

            val isValid = fullName.isNotEmpty() &&
                    bio.isNotEmpty() &&
                    binding.countrySpinner.selectedItemPosition > 0

            if (!isValid) {
                Toast.makeText(this, "Please fill in all the required fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val changedFields = hashMapOf<String, Any>()
            val old = existingProfileData ?: emptyMap()

            if (old["name"] != fullName) changedFields["name"] = fullName
            if (old["bio"] != bio) changedFields["bio"] = bio
            if (old["country"] != country) changedFields["country"] = country
            if ((old["plantingGoal"] as? Long)?.toInt() != plantingGoal) changedFields["plantingGoal"] =
                plantingGoal

            val email = FirebaseAuth.getInstance().currentUser?.email
            val username = email?.split("@")?.firstOrNull() ?: "Guest"

            if (old["username"] != userName) changedFields["username"] = username

            if (isImageSelected) {
                loader.title.text = "Uploading..."
                loader.message.text = "Uploading new profile image..."
                loader.dialog.show()
                uploadProfileImageWithDiff(changedFields)
            } else {
                if (changedFields.isNotEmpty()) {
                    saveUserProfileDiff(changedFields)
                } else {
                    getSharedPreferences("VanamPrefs", MODE_PRIVATE).edit()
                        .putBoolean("isBasicProfileComplete", true).apply()

                    startActivity(Intent(this, InterestActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun uploadProfileImageWithDiff(changedFields: HashMap<String, Any>) {
        val uid = FirebaseAuth.getInstance().uid ?: return

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
        val uid = FirebaseAuth.getInstance().uid ?: return

        userCollection.document(uid).set(changes, SetOptions.merge()).addOnSuccessListener {
            getSharedPreferences("VanamPrefs", MODE_PRIVATE).edit()
                .putBoolean("isBasicProfileComplete", true).apply()

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
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countries)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.countrySpinner.adapter = adapter
    }

    private fun fetchAndAutofillProfile() {
        val uid = FirebaseAuth.getInstance().uid ?: return

        loader.title.text = "Loading..."
        loader.message.text = "Fetching your profile data..."
        loader.dialog.show()

        userCollection.document(uid).get().addOnSuccessListener { document ->
            existingProfileData = document.data

            val user = FirebaseAuth.getInstance().currentUser
            val name = document.getString("name") ?: user?.displayName.toString()
            val profileImage = document.getString("profileImage") ?: user?.photoUrl.toString()
            val username = document.getString("username") ?: user?.email?.split("@")?.firstOrNull()

            binding.fullName.setText(name)
            binding.bioText.setText(document.getString("bio"))
            binding.username.setText(username)

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
}
