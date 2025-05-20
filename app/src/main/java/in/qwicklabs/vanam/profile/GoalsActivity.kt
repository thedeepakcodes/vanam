package `in`.qwicklabs.vanam.profile

import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import `in`.qwicklabs.vanam.Dashboard
import `in`.qwicklabs.vanam.databinding.ActivityCompleteGoalsBinding
import `in`.qwicklabs.vanam.utils.Loader

class GoalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompleteGoalsBinding
    private lateinit var gardeningCards: List<MaterialCardView>
    private lateinit var commitmentOptions: List<MaterialButton>
    private lateinit var loader: Loader

    private val firestore = FirebaseFirestore.getInstance()
    private val userCollection by lazy {
        firestore.collection("Vanam").document("Users").collection("Profile")
    }

    private var gardeningPref: Int? = null
    private var commitmentGoal: Int? = null
    private var weeklyHours: Int = 6

    private var prevGardening: String? = null
    private var prevCommitment: Int? = null
    private var prevWeeklyHours: Int? = null

    private val defaultColor = "#E6FFF2".toColorInt()
    private val selectedColor = "#CCEBD7".toColorInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompleteGoalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (getSharedPreferences("VanamPrefs", MODE_PRIVATE)
                .getBoolean("isProfileComplete", false)
        ) {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
            return
        }

        setupGardeningObj()
        setupCommitmentGoal()
        setupWeeklyCommitment()
        setupComplete()

        // Load previously saved data
        FirebaseAuth.getInstance().uid?.let { uid ->
            userCollection.document(uid).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    prevGardening = doc.getString("gardeningPreference")
                    prevCommitment = doc.getLong("commitmentGoal")?.toInt()
                    prevWeeklyHours = doc.getLong("weeklyHours")?.toInt()
                    prefillSelections()
                }
            }
        }
    }

    private fun setupGardeningObj() {
        gardeningCards = listOf(
            binding.cardHobbyGardening,
            binding.cardCommercialGrowing,
            binding.cardSelfSustaining,
            binding.cardCommunityGarden
        )

        gardeningCards.forEach { card ->
            card.setOnClickListener {
                gardeningPref = card.id
                highlightSelection(card.id, gardeningCards)
            }
        }
    }

    private fun setupCommitmentGoal() {
        commitmentOptions = listOf(
            binding.btn3Months, binding.btn6Months, binding.btn1Year, binding.btnLongTerm
        )

        commitmentOptions.forEach { button ->
            button.setOnClickListener {
                commitmentGoal = button.id
                highlightSelection(button.id, commitmentOptions)
            }
        }
    }

    private fun setupWeeklyCommitment() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                weeklyHours = progress
                binding.tvHoursPerWeek.text = "$progress hours/week"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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

    private fun getGardeningPref(): String? {
        return when (gardeningPref) {
            binding.cardHobbyGardening.id -> "Hobby Gardening"
            binding.cardCommercialGrowing.id -> "Commercial Growing"
            binding.cardSelfSustaining.id -> "Self-Sustaining"
            binding.cardCommunityGarden.id -> "Community Garden"
            else -> null
        }
    }

    private fun getCommitmentGoal(): Int? {
        return when (commitmentGoal) {
            binding.btn3Months.id -> 3
            binding.btn6Months.id -> 6
            binding.btn1Year.id -> 12
            binding.btnLongTerm.id -> 240
            else -> null
        }
    }

    private fun prefillSelections() {
        val gardening = prevGardening
        val commitment = prevCommitment
        val hours = prevWeeklyHours

        // Gardening Preference
        gardeningPref = when (gardening) {
            "Hobby Gardening" -> binding.cardHobbyGardening.id
            "Commercial Growing" -> binding.cardCommercialGrowing.id
            "Self-Sustaining" -> binding.cardSelfSustaining.id
            "Community Garden" -> binding.cardCommunityGarden.id
            else -> null
        }?.also { highlightSelection(it, gardeningCards) }

        // Commitment Goal
        commitmentGoal = when (commitment) {
            3 -> binding.btn3Months.id
            6 -> binding.btn6Months.id
            12 -> binding.btn1Year.id
            240 -> binding.btnLongTerm.id
            else -> null
        }?.also { highlightSelection(it, commitmentOptions) }

        // Weekly Hours
        hours?.let {
            weeklyHours = it
            binding.seekBar.progress = it
            binding.tvHoursPerWeek.text = "$it hours/week"
        }
    }

    private fun setupComplete() {
        binding.cardCompleteButton.setOnClickListener {
            if (gardeningPref == null || commitmentGoal == null) {
                Toast.makeText(this, "Please select all preferences.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loader = Loader(this).apply {
                title.text = "Saving..."
                message.text = "Please wait while we save your preferences."
                dialog.show()
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId == null) {
                loader.dialog.dismiss()
                Toast.makeText(this, "Please Login to continue", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gardening = getGardeningPref()
            val commitment = getCommitmentGoal()

            if (gardening == null || commitment == null) {
                loader.dialog.dismiss()
                Toast.makeText(this, "Invalid selection. Please try again.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val hasChanged = gardening != prevGardening ||
                    commitment != prevCommitment ||
                    weeklyHours != prevWeeklyHours

            if (!hasChanged) {
                loader.dialog.dismiss()
                markProfileCompleteAndMove()
                return@setOnClickListener
            }

            val data = hashMapOf(
                "gardeningPreference" to gardening,
                "commitmentGoal" to commitment,
                "weeklyHours" to weeklyHours,
                "isProfileSetupComplete" to true
            )

            userCollection.document(userId).set(data, SetOptions.merge()).addOnSuccessListener {
                loader.dialog.dismiss()
                markProfileCompleteAndMove()
            }.addOnFailureListener { e ->
                loader.dialog.dismiss()
                Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun markProfileCompleteAndMove() {
        getSharedPreferences("VanamPrefs", MODE_PRIVATE).edit()
            .putBoolean("isProfileComplete", true).apply()

        startActivity(Intent(this, Dashboard::class.java))
        finish()
    }
}
