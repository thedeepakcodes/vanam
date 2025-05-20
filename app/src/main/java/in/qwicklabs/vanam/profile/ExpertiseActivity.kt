package `in`.qwicklabs.vanam.profile

import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import `in`.qwicklabs.vanam.databinding.ActivityCompleteExpertiseBinding
import `in`.qwicklabs.vanam.utils.Loader
import kotlin.coroutines.cancellation.CancellationException

class ExpertiseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompleteExpertiseBinding
    private lateinit var loader: Loader
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val userCollection by lazy {
        firestore.collection("Vanam").document("Users").collection("Profile")
    }

    private var expLevel: Int? = null
    private var expYears: Int? = null

    private var previousLevel: String? = null
    private var previousYears: Int? = null

    private val defaultColor = "#E6FFF2".toColorInt()
    private val selectedColor = "#CCEBD7".toColorInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCompleteExpertiseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (getSharedPreferences("VanamPrefs", MODE_PRIVATE)
                .getBoolean("isProfileExpertiseComplete", false)
        ) {
            startActivity(Intent(this, GoalsActivity::class.java))
            finish()
            return
        }

        loader = Loader(this)

        setupExperienceLevelCards()
        setupExperienceYears()
        setupNextButton()

        // Load previously saved data
        FirebaseAuth.getInstance().uid?.let { uid ->
            userCollection.document(uid).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    previousLevel = document.getString("expertiseLevel")
                    previousYears = document.getLong("experienceYears")?.toInt()
                    prefillSelections()
                }
            }
        }
    }

    private fun setupExperienceLevelCards() {
        val expCards = listOf(
            binding.cardBeginner, binding.cardIntermediate, binding.cardAdvanced, binding.cardExpert
        )

        expCards.forEachIndexed { index, card ->
            card.setOnClickListener {
                expLevel = index
                card.setCardBackgroundColor(selectedColor)
                expCards.filter { it != card }.forEach {
                    it.setCardBackgroundColor(defaultColor)
                }
            }
        }
    }

    private fun setupExperienceYears() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.yearsText.text = "$progress years"
                expYears = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupNextButton() {
        binding.nextButton.setOnClickListener {
            loader.title.text = "Updating..."
            loader.message.text = "Please wait while we update your experience level."
            loader.dialog.show()

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                showErrorAndExit("User not logged in")
            }

            val level = expLevel
            if (level == null) {
                showErrorAndExit("Please select an experience level")
            }

            val years = expYears
            if (years == null) {
                showErrorAndExit("Please select your years of experience")
            }

            val levelString = when (level) {
                0 -> "Beginner"
                1 -> "Intermediate"
                2 -> "Advanced"
                3 -> "Expert"
                else -> "Unknown"
            }

            val hasChanged = levelString != previousLevel || years != previousYears

            if (!hasChanged) {
                loader.dialog.dismiss()
                markProfileCompleteAndMove()
                return@setOnClickListener
            }

            val experienceMap = hashMapOf(
                "expertiseLevel" to levelString,
                "experienceYears" to years
            )

            userCollection.document(userId).set(experienceMap, SetOptions.merge())
                .addOnSuccessListener {
                    loader.dialog.dismiss()
                    markProfileCompleteAndMove()
                }
                .addOnFailureListener { e ->
                    showErrorAndExit("Failed to update profile: ${e.localizedMessage}")
                }
        }
    }

    private fun prefillSelections() {
        // Map previously saved level string to index
        val levelIndex = when (previousLevel) {
            "Beginner" -> 0
            "Intermediate" -> 1
            "Advanced" -> 2
            "Expert" -> 3
            else -> null
        }

        expLevel = levelIndex
        val cards = listOf(
            binding.cardBeginner, binding.cardIntermediate, binding.cardAdvanced, binding.cardExpert
        )

        levelIndex?.let { index ->
            cards[index].setCardBackgroundColor(selectedColor)
            cards.filterIndexed { i, _ -> i != index }.forEach {
                it.setCardBackgroundColor(defaultColor)
            }
        }

        previousYears?.let {
            binding.seekBar.progress = it
            binding.yearsText.text = "$it years"
            expYears = it
        }
    }

    private fun markProfileCompleteAndMove() {
        getSharedPreferences("VanamPrefs", MODE_PRIVATE).edit()
            .putBoolean("isProfileExpertiseComplete", true).apply()

        startActivity(Intent(this, GoalsActivity::class.java))
        finish()
    }

    private fun showErrorAndExit(message: String): Nothing {
        loader.dialog.dismiss()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        throw CancellationException(message)
    }
}
