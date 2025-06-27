package `in`.qwicklabs.vanam.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import `in`.qwicklabs.vanam.databinding.ActivityInterestBinding
import `in`.qwicklabs.vanam.model.User
import `in`.qwicklabs.vanam.repository.UserRepository
import `in`.qwicklabs.vanam.utils.Loader
import kotlinx.coroutines.launch

class InterestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInterestBinding
    private lateinit var loadingDialog: Loader

    private val sharedPrefs by lazy { getSharedPreferences("VanamPrefs", MODE_PRIVATE) }

    private lateinit var treePreferenceCards: List<MaterialCardView>
    private lateinit var seasonOptionButtons: List<MaterialButton>

    private val unselectedColor = "#E6FFF2".toColorInt()
    private val highlightedColor = "#CCEBD7".toColorInt()
    private var selectedTreePreferenceId: Int? = null
    private var selectedSeasonId: Int? = null

    // Previously saved values
    private var savedTreePreference: String? = null
    private var savedSeasonPreference: String? = null
    private var savedExperienceLevel: Int? = null
    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityInterestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, insets.top, view.paddingRight, insets.bottom)
            windowInsets
        }

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            true

        if (sharedPrefs.getBoolean("ProfileSetup_2", false)) {
            startActivity(Intent(this, ExpertiseActivity::class.java))
            finish()
        }

        setupTreePreferenceCards()
        setupPlantingSeasonButtons()
        setupExperienceLevelSeekBar()
        setupSaveAndContinueButton()

        lifecycleScope.launch {
            user = UserRepository.getUser()
            savedTreePreference = user?.plantTypeInterest
            savedSeasonPreference = user?.preferredSeason
            savedExperienceLevel = user?.expRating
            prefillSavedPreferences()
        }
    }

    private fun setupTreePreferenceCards() {
        treePreferenceCards = listOf(
            binding.cardFruitTrees,
            binding.cardFastGrowing,
            binding.cardShadeTrees,
            binding.cardMedicinalTrees
        )

        treePreferenceCards.forEach { card ->
            card.setOnClickListener {
                selectedTreePreferenceId = card.id
                highlightSelectedView(card.id, treePreferenceCards)
            }
        }
    }

    private fun setupPlantingSeasonButtons() {
        seasonOptionButtons = listOf(
            binding.btnSpring, binding.btnSummer, binding.btnFall, binding.btnWinter
        )

        seasonOptionButtons.forEach { button ->
            button.setOnClickListener {
                selectedSeasonId = button.id
                highlightSelectedView(button.id, seasonOptionButtons)
            }
        }
    }

    private fun <T> highlightSelectedView(selectedId: Int, viewList: List<T>) {
        viewList.forEach { view ->
            when (view) {
                is MaterialCardView -> {
                    view.setCardBackgroundColor(unselectedColor)
                    if (view.id == selectedId) view.setCardBackgroundColor(highlightedColor)
                }

                is MaterialButton -> {
                    view.setBackgroundColor(unselectedColor)
                    if (view.id == selectedId) view.setBackgroundColor(highlightedColor)
                }
            }
        }
    }

    private fun setupExperienceLevelSeekBar() {
        binding.seekBarExperience.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.textExperienceValue.text = "$progress/5"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    @SuppressLint("UseKtx")
    private fun setupSaveAndContinueButton() {
        binding.saveContinue.setOnClickListener {
            loadingDialog = Loader(this).apply {
                title.text = "Saving Preferences.."
                message.text = "Please wait while we save your preferences."
                dialog.show()
            }

            val newTreePreference = getTreePreferenceFromSelection()
            val newSeasonPreference = getSeasonPreferenceFromSelection()
            val newExperienceLevel = binding.seekBarExperience.progress

            if (newTreePreference == null || newSeasonPreference == null) {
                loadingDialog.dialog.dismiss()
                Toast.makeText(
                    this, "Please select a tree preference and planting season.", Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val hasChanges =
                newTreePreference != savedTreePreference || newSeasonPreference != savedSeasonPreference || newExperienceLevel != savedExperienceLevel

            if (!hasChanges) {
                loadingDialog.dialog.dismiss()
                markProfileCompleteAndProceed()
                return@setOnClickListener
            }

            user?.plantTypeInterest = newTreePreference
            user?.preferredSeason = newSeasonPreference
            user?.expRating = newExperienceLevel

            lifecycleScope.launch {
                try {
                    user?.let { it1 -> UserRepository.updateUser(it1) }
                    loadingDialog.dialog.dismiss()
                    markProfileCompleteAndProceed()
                } catch (e: Exception) {
                    loadingDialog.dialog.dismiss()

                    Toast.makeText(
                        this@InterestActivity,
                        "Failed to save profile: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun markProfileCompleteAndProceed() {

        sharedPrefs.edit() { putBoolean("ProfileSetup_2", true) }

        startActivity(Intent(this, ExpertiseActivity::class.java))
        finish()
    }

    private fun prefillSavedPreferences() {
        selectedTreePreferenceId = when (savedTreePreference) {
            "Fruit Trees" -> binding.cardFruitTrees.id
            "Fast Growing" -> binding.cardFastGrowing.id
            "Shade Trees" -> binding.cardShadeTrees.id
            "Medicinal Trees" -> binding.cardMedicinalTrees.id
            else -> null
        }?.also {
            highlightSelectedView(it, treePreferenceCards)
        }

        selectedSeasonId = when (savedSeasonPreference) {
            "Spring" -> binding.btnSpring.id
            "Summer" -> binding.btnSummer.id
            "Fall" -> binding.btnFall.id
            "Winter" -> binding.btnWinter.id
            else -> null
        }?.also {
            highlightSelectedView(it, seasonOptionButtons)
        }

        savedExperienceLevel?.let {
            binding.seekBarExperience.progress = it
            binding.textExperienceValue.text = "$it/5"
        }
    }

    private fun getTreePreferenceFromSelection(): String? {
        return when (selectedTreePreferenceId) {
            binding.cardFruitTrees.id -> "Fruit Trees"
            binding.cardFastGrowing.id -> "Fast Growing"
            binding.cardShadeTrees.id -> "Shade Trees"
            binding.cardMedicinalTrees.id -> "Medicinal Trees"
            else -> null
        }
    }

    private fun getSeasonPreferenceFromSelection(): String? {
        return when (selectedSeasonId) {
            binding.btnSpring.id -> "Spring"
            binding.btnSummer.id -> "Summer"
            binding.btnFall.id -> "Fall"
            binding.btnWinter.id -> "Winter"
            else -> null
        }
    }
}
