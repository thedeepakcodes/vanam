package `in`.qwicklabs.vanam.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import `in`.qwicklabs.vanam.databinding.ActivityInterestBinding
import `in`.qwicklabs.vanam.utils.Loader

class InterestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInterestBinding
    private lateinit var preferenceCards: List<MaterialCardView>
    private lateinit var plantingSeasonButtons: List<MaterialButton>
    private lateinit var loader: Loader
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val userCollection by lazy {
        firestore.collection("Vanam").document("Users").collection("Profile")
    }

    private val defaultColor = "#E6FFF2".toColorInt()
    private val selectedColor = "#CCEBD7".toColorInt()
    private var selectedPreference: Int? = null
    private var selectedSeason: Int? = null

    // Previously saved values
    private var previousPreference: String? = null
    private var previousSeason: String? = null
    private var previousExperience: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityInterestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Skip if already complete
        if (getSharedPreferences("VanamPrefs", MODE_PRIVATE)
                .getBoolean("isProfileInterestComplete", false)
        ) {
            startActivity(Intent(this, ExpertiseActivity::class.java))
            finish()
            return
        }

        setupPreferenceCards()
        setupSeasonButtons()
        setupExperienceLevel()
        setupSaveButton()

        // Load saved data from Firestore
        FirebaseAuth.getInstance().uid?.let { uid ->
            userCollection.document(uid).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    previousPreference = document.getString("preferredTree")
                    previousSeason = document.getString("preferredSeason")
                    previousExperience = document.getLong("experienceLevel")?.toInt()
                    prefillSelections()
                }
            }
        }
    }

    private fun setupPreferenceCards() {
        preferenceCards = listOf(
            binding.cardFruitTrees,
            binding.cardFastGrowing,
            binding.cardShadeTrees,
            binding.cardMedicinalTrees
        )

        preferenceCards.forEach { card ->
            card.setOnClickListener {
                selectedPreference = card.id
                highlightSelection(card.id, preferenceCards)
            }
        }
    }

    private fun setupSeasonButtons() {
        plantingSeasonButtons = listOf(
            binding.btnSpring, binding.btnSummer, binding.btnFall, binding.btnWinter
        )

        plantingSeasonButtons.forEach { button ->
            button.setOnClickListener {
                selectedSeason = button.id
                highlightSelection(button.id, plantingSeasonButtons)
            }
        }
    }

    private fun <T> highlightSelection(selectedId: Int, views: List<T>) {
        views.forEach { view ->
            when (view) {
                is MaterialCardView -> {
                    view.setCardBackgroundColor(defaultColor)
                    if (view.id == selectedId) view.setCardBackgroundColor(selectedColor)
                }

                is MaterialButton -> {
                    view.setBackgroundColor(defaultColor)
                    if (view.id == selectedId) view.setBackgroundColor(selectedColor)
                }
            }
        }
    }

    private fun setupExperienceLevel() {
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
    private fun setupSaveButton() {
        binding.saveContinue.setOnClickListener {
            loader = Loader(this).apply {
                title.text = "Uploading..."
                message.text = "Please wait while we upload your profile."
                dialog.show()
            }

            val newPreference = getSelectedPreference()
            val newSeason = getSelectedSeason()
            val newExperience = binding.seekBarExperience.progress

            if (newPreference == null || newSeason == null) {
                loader.dialog.dismiss()
                Toast.makeText(
                    this,
                    "Please select a tree preference and planting season.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Detect changes
            val hasChanged = newPreference != previousPreference ||
                    newSeason != previousSeason ||
                    newExperience != previousExperience

            if (!hasChanged) {
                loader.dialog.dismiss()
                markProfileCompleteAndMove()
                return@setOnClickListener
            }

            // Save only if changed
            val profileData = hashMapOf(
                "preferredTree" to newPreference,
                "preferredSeason" to newSeason,
                "experienceLevel" to newExperience
            )

            FirebaseAuth.getInstance().uid?.let { uid ->
                userCollection.document(uid).set(profileData, SetOptions.merge())
                    .addOnSuccessListener {
                        loader.dialog.dismiss()
                        markProfileCompleteAndMove()
                    }.addOnFailureListener { e ->
                        loader.dialog.dismiss()
                        Toast.makeText(
                            this,
                            "Failed to save profile: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }
    }

    private fun markProfileCompleteAndMove() {
        getSharedPreferences("VanamPrefs", MODE_PRIVATE).edit()
            .putBoolean("isProfileInterestComplete", true).apply()

        startActivity(Intent(this, ExpertiseActivity::class.java))
        finish()
    }

    private fun prefillSelections() {
        // Preference
        selectedPreference = when (previousPreference) {
            "Fruit Trees" -> binding.cardFruitTrees.id
            "Fast Growing" -> binding.cardFastGrowing.id
            "Shade Trees" -> binding.cardShadeTrees.id
            "Medicinal Trees" -> binding.cardMedicinalTrees.id
            else -> null
        }?.also {
            highlightSelection(it, preferenceCards)
        }

        // Season
        selectedSeason = when (previousSeason) {
            "Spring" -> binding.btnSpring.id
            "Summer" -> binding.btnSummer.id
            "Fall" -> binding.btnFall.id
            "Winter" -> binding.btnWinter.id
            else -> null
        }?.also {
            highlightSelection(it, plantingSeasonButtons)
        }

        // Experience
        previousExperience?.let {
            binding.seekBarExperience.progress = it
            binding.textExperienceValue.text = "$it/5"
        }
    }

    private fun getSelectedPreference(): String? {
        return when (selectedPreference) {
            binding.cardFruitTrees.id -> "Fruit Trees"
            binding.cardFastGrowing.id -> "Fast Growing"
            binding.cardShadeTrees.id -> "Shade Trees"
            binding.cardMedicinalTrees.id -> "Medicinal Trees"
            else -> null
        }
    }

    private fun getSelectedSeason(): String? {
        return when (selectedSeason) {
            binding.btnSpring.id -> "Spring"
            binding.btnSummer.id -> "Summer"
            binding.btnFall.id -> "Fall"
            binding.btnWinter.id -> "Winter"
            else -> null
        }
    }
}
