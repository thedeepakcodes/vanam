package `in`.qwicklabs.vanam.profile

import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import `in`.qwicklabs.vanam.databinding.ActivityCompleteExpertiseBinding
import `in`.qwicklabs.vanam.utils.Loader

class ExpertiseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompleteExpertiseBinding
    private lateinit var loader: Loader

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firebaseFirestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val userProfileCollection by lazy {
        firebaseFirestore.collection("Vanam").document("Users").collection("Profile")
    }

    private var selectedExperienceLevelIndex: Int? = null
    private var selectedYearsOfExperience: Int? = 0

    private var previouslySavedLevel: String? = null
    private var previouslySavedYears: Int? = null

    private val cardDefaultColor = "#E6FFF2".toColorInt()
    private val cardSelectedColor = "#CCEBD7".toColorInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCompleteExpertiseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loader = Loader(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, insets.top, view.paddingRight, insets.bottom)
            windowInsets
        }

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            true

        if (getSharedPreferences("VanamPrefs", MODE_PRIVATE)
                .getBoolean("isProfileExpertiseComplete", false)
        ) {
            navigateToGoalsScreen()
            return
        }

        setupExperienceLevelCardListeners()
        setupSeekBarListener()
        setupNextButtonClickListener()

        // Load previously saved user expertise data
        firebaseAuth.uid?.let { userId ->
            userProfileCollection.document(userId).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    previouslySavedLevel = document.getString("expertiseLevel")
                    previouslySavedYears = document.getLong("experienceYears")?.toInt()
                    prefillUserSelections()
                }
            }
        }
    }

    private fun setupExperienceLevelCardListeners() {
        val levelCards = listOf(
            binding.cardBeginner,
            binding.cardIntermediate,
            binding.cardAdvanced,
            binding.cardExpert
        )

        levelCards.forEachIndexed { index, cardView ->
            cardView.setOnClickListener {
                selectedExperienceLevelIndex = index
                cardView.setCardBackgroundColor(cardSelectedColor)
                levelCards.filter { it != cardView }.forEach {
                    it.setCardBackgroundColor(cardDefaultColor)
                }
            }
        }
    }

    private fun setupSeekBarListener() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.yearsText.text = "$progress years"
                selectedYearsOfExperience = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupNextButtonClickListener() {
        binding.nextButton.setOnClickListener {
            loader.title.text = "Updating..."
            loader.message.text = "Please wait while we update your experience level."
            loader.dialog.show()

            val userId = firebaseAuth.currentUser?.uid

            if (userId == null) {
                showError("User not logged in")
                return@setOnClickListener
            }

            val levelIndex = selectedExperienceLevelIndex
            if (levelIndex == null) {
                showError("Please select an experience level")
                return@setOnClickListener
            }

            val experienceYears = selectedYearsOfExperience

            if (experienceYears == null) {
                showError("Please select your years of experience")
                return@setOnClickListener
            }

            val levelString = when (levelIndex) {
                0 -> "Beginner"
                1 -> "Intermediate"
                2 -> "Advanced"
                3 -> "Expert"
                else -> "Unknown"
            }

            val hasUserUpdatedData =
                levelString != previouslySavedLevel || experienceYears != previouslySavedYears

            if (!hasUserUpdatedData) {
                loader.dialog.dismiss()
                markProfileCompleteAndProceed()
                return@setOnClickListener
            }

            val updatedExpertiseData = hashMapOf(
                "expertiseLevel" to levelString,
                "experienceYears" to experienceYears
            )

            userProfileCollection.document(userId).set(updatedExpertiseData, SetOptions.merge())
                .addOnSuccessListener {
                    loader.dialog.dismiss()
                    markProfileCompleteAndProceed()
                }
                .addOnFailureListener { error ->
                    showError("Failed to update profile: ${error.localizedMessage}")
                }
        }
    }

    private fun prefillUserSelections() {
        val levelIndex = when (previouslySavedLevel) {
            "Beginner" -> 0
            "Intermediate" -> 1
            "Advanced" -> 2
            "Expert" -> 3
            else -> null
        }

        selectedExperienceLevelIndex = levelIndex

        val levelCards = listOf(
            binding.cardBeginner,
            binding.cardIntermediate,
            binding.cardAdvanced,
            binding.cardExpert
        )

        levelIndex?.let { selectedIndex ->
            levelCards[selectedIndex].setCardBackgroundColor(cardSelectedColor)
            levelCards.filterIndexed { i, _ -> i != selectedIndex }.forEach {
                it.setCardBackgroundColor(cardDefaultColor)
            }
        }

        previouslySavedYears?.let { savedYears ->
            binding.seekBar.progress = savedYears
            binding.yearsText.text = "$savedYears years"
            selectedYearsOfExperience = savedYears
        }
    }

    private fun markProfileCompleteAndProceed() {
        getSharedPreferences("VanamPrefs", MODE_PRIVATE).edit {
            putBoolean("isProfileExpertiseComplete", true)
        }
        navigateToGoalsScreen()
    }

    private fun navigateToGoalsScreen() {
        startActivity(Intent(this, GoalsActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        loader.dialog.dismiss()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
