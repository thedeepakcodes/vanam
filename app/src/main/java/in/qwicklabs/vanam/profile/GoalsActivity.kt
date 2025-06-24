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
    private lateinit var gardeningPreferenceCards: List<MaterialCardView>
    private lateinit var commitmentDurationButtons: List<MaterialButton>
    private lateinit var loadingDialog: Loader

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val usersProfileCollection by lazy {
        firestoreInstance.collection("Vanam").document("Users").collection("Profile")
    }

    private var selectedGardeningPreferenceCardId: Int? = null
    private var selectedCommitmentDurationButtonId: Int? = null
    private var selectedWeeklyHours: Int = 6

    private var previousGardeningPreference: String? = null
    private var previousCommitmentDuration: Int? = null
    private var previousWeeklyHours: Int? = null

    private val defaultCardColor = "#E6FFF2".toColorInt()
    private val selectedCardColor = "#CCEBD7".toColorInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompleteGoalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, insets.top, view.paddingRight, insets.bottom)
            windowInsets
        }

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            true

        if (getSharedPreferences("VanamPrefs", MODE_PRIVATE)
                .getBoolean("isProfileComplete", false)
        ) {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
            return
        }

        setupGardeningPreferenceCards()
        setupCommitmentDurationButtons()
        setupWeeklyHoursSeekBar()
        setupCompleteButton()

        // Load previously saved user data
        firebaseAuth.uid?.let { uid ->
            usersProfileCollection.document(uid).get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    previousGardeningPreference = documentSnapshot.getString("gardeningPreference")
                    previousCommitmentDuration = documentSnapshot.getLong("commitmentGoal")?.toInt()
                    previousWeeklyHours = documentSnapshot.getLong("weeklyHours")?.toInt()
                    prefillUserSelections()
                }
            }
        }
    }

    private fun setupGardeningPreferenceCards() {
        gardeningPreferenceCards = listOf(
            binding.cardHobbyGardening,
            binding.cardCommercialGrowing,
            binding.cardSelfSustaining,
            binding.cardCommunityGarden
        )

        gardeningPreferenceCards.forEach { card ->
            card.setOnClickListener {
                selectedGardeningPreferenceCardId = card.id
                updateSelectionHighlight(card.id, gardeningPreferenceCards)
            }
        }
    }

    private fun setupCommitmentDurationButtons() {
        commitmentDurationButtons = listOf(
            binding.btn3Months, binding.btn6Months, binding.btn1Year, binding.btnLongTerm
        )

        commitmentDurationButtons.forEach { button ->
            button.setOnClickListener {
                selectedCommitmentDurationButtonId = button.id
                updateSelectionHighlight(button.id, commitmentDurationButtons)
            }
        }
    }

    private fun setupWeeklyHoursSeekBar() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedWeeklyHours = progress
                binding.tvHoursPerWeek.text = "$progress hours/week"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun <T> updateSelectionHighlight(selectedId: Int, views: List<T>) {
        views.forEach { view ->
            when (view) {
                is MaterialCardView -> {
                    view.setCardBackgroundColor(defaultCardColor)
                    if (view.id == selectedId) view.setCardBackgroundColor(selectedCardColor)
                }

                is MaterialButton -> {
                    view.setBackgroundColor(defaultCardColor)
                    if (view.id == selectedId) view.setBackgroundColor(selectedCardColor)
                }
            }
        }
    }

    private fun mapCardIdToGardeningPreference(): String? {
        return when (selectedGardeningPreferenceCardId) {
            binding.cardHobbyGardening.id -> "Hobby Gardening"
            binding.cardCommercialGrowing.id -> "Commercial Growing"
            binding.cardSelfSustaining.id -> "Self-Sustaining"
            binding.cardCommunityGarden.id -> "Community Garden"
            else -> null
        }
    }

    private fun mapButtonIdToCommitmentDuration(): Int? {
        return when (selectedCommitmentDurationButtonId) {
            binding.btn3Months.id -> 3
            binding.btn6Months.id -> 6
            binding.btn1Year.id -> 12
            binding.btnLongTerm.id -> 240
            else -> null
        }
    }

    private fun prefillUserSelections() {
        // Prefill gardening preference
        selectedGardeningPreferenceCardId = when (previousGardeningPreference) {
            "Hobby Gardening" -> binding.cardHobbyGardening.id
            "Commercial Growing" -> binding.cardCommercialGrowing.id
            "Self-Sustaining" -> binding.cardSelfSustaining.id
            "Community Garden" -> binding.cardCommunityGarden.id
            else -> null
        }?.also { updateSelectionHighlight(it, gardeningPreferenceCards) }

        // Prefill commitment duration
        selectedCommitmentDurationButtonId = when (previousCommitmentDuration) {
            3 -> binding.btn3Months.id
            6 -> binding.btn6Months.id
            12 -> binding.btn1Year.id
            240 -> binding.btnLongTerm.id
            else -> null
        }?.also { updateSelectionHighlight(it, commitmentDurationButtons) }

        // Prefill weekly hours
        previousWeeklyHours?.let {
            selectedWeeklyHours = it
            binding.seekBar.progress = it
            binding.tvHoursPerWeek.text = "$it hours/week"
        }
    }

    private fun setupCompleteButton() {
        binding.cardCompleteButton.setOnClickListener {
            if (selectedGardeningPreferenceCardId == null || selectedCommitmentDurationButtonId == null) {
                Toast.makeText(this, "Please select all preferences.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadingDialog = Loader(this).apply {
                title.text = "Saving..."
                message.text = "Please wait while we save your preferences."
                dialog.show()
            }

            val currentUserId = firebaseAuth.currentUser?.uid

            if (currentUserId == null) {
                loadingDialog.dialog.dismiss()
                Toast.makeText(this, "Please Login to continue", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gardeningPreference = mapCardIdToGardeningPreference()
            val commitmentDuration = mapButtonIdToCommitmentDuration()

            if (gardeningPreference == null || commitmentDuration == null) {
                loadingDialog.dialog.dismiss()
                Toast.makeText(this, "Invalid selection. Please try again.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val hasUserMadeChanges = gardeningPreference != previousGardeningPreference ||
                    commitmentDuration != previousCommitmentDuration ||
                    selectedWeeklyHours != previousWeeklyHours

            if (!hasUserMadeChanges) {
                loadingDialog.dialog.dismiss()
                completeProfileAndNavigateToDashboard()
                return@setOnClickListener
            }

            val updatedData = hashMapOf(
                "gardeningPreference" to gardeningPreference,
                "commitmentGoal" to commitmentDuration,
                "weeklyHours" to selectedWeeklyHours,
                "isProfileSetupComplete" to true
            )

            usersProfileCollection.document(currentUserId).set(updatedData, SetOptions.merge())
                .addOnSuccessListener {
                    loadingDialog.dialog.dismiss()
                    completeProfileAndNavigateToDashboard()
                }
                .addOnFailureListener { exception ->
                    loadingDialog.dialog.dismiss()
                    Toast.makeText(this, "Failed to save: ${exception.message}", Toast.LENGTH_LONG)
                        .show()
                }
        }
    }

    private fun completeProfileAndNavigateToDashboard() {
        getSharedPreferences("VanamPrefs", MODE_PRIVATE).edit {
            putBoolean("isProfileComplete", true)
        }

        startActivity(Intent(this, Dashboard::class.java))
        finish()
    }
}
