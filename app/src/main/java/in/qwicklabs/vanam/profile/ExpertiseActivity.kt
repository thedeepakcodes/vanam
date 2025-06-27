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
import androidx.lifecycle.lifecycleScope
import `in`.qwicklabs.vanam.databinding.ActivityCompleteExpertiseBinding
import `in`.qwicklabs.vanam.model.User
import `in`.qwicklabs.vanam.repository.FirebaseRepository
import `in`.qwicklabs.vanam.repository.UserRepository
import `in`.qwicklabs.vanam.utils.Loader
import kotlinx.coroutines.launch

class ExpertiseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompleteExpertiseBinding
    private lateinit var loader: Loader
    private val sharedPrefs by lazy { getSharedPreferences("VanamPrefs", MODE_PRIVATE) }
    private var currentUser: User? = null
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

        if (sharedPrefs.getBoolean("ProfileSetup_3", false)) {
            startActivity(Intent(this, GoalsActivity::class.java))
            finish()
        }

        setupExperienceLevelCardListeners()
        setupSeekBarListener()
        setupNextButtonClickListener()

        // Load previously saved user expertise data
        lifecycleScope.launch {
            currentUser = UserRepository.getUser()
            previouslySavedLevel = currentUser?.expLevel
            previouslySavedYears = currentUser?.expYear
            prefillUserSelections()
        }
    }

    private fun setupExperienceLevelCardListeners() {
        val levelCards = listOf(
            binding.cardBeginner, binding.cardIntermediate, binding.cardAdvanced, binding.cardExpert
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

            val userId = FirebaseRepository.getCurrentUserId()

            if (userId == "") {
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

            currentUser?.expLevel = levelString
            currentUser?.expYear = experienceYears

            lifecycleScope.launch {
                try {
                    currentUser?.let { it1 -> UserRepository.updateUser(it1) }
                    loader.dialog.dismiss()
                    markProfileCompleteAndProceed()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@ExpertiseActivity,
                        "Failed to update profile: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
            binding.cardBeginner, binding.cardIntermediate, binding.cardAdvanced, binding.cardExpert
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
        sharedPrefs.edit { putBoolean("ProfileSetup_3", true) }
        startActivity(Intent(this@ExpertiseActivity, GoalsActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        loader.dialog.dismiss()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

