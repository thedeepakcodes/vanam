package `in`.qwicklabs.vanam.activities

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.databinding.ActivityViewTreeBinding
import `in`.qwicklabs.vanam.utils.UtilityFunctions
import `in`.qwicklabs.vanam.viewModel.TreeViewModel

class ViewTree : AppCompatActivity() {
    private lateinit var binding: ActivityViewTreeBinding
    private val treeViewModel: TreeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityViewTreeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val treeId = intent.getStringExtra("treeId")
        if (!treeId.isNullOrEmpty()) {
            loadTreeData(treeId)
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupUI()
    }

    private fun setupUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private fun loadTreeData(treeId: String) {
        treeViewModel.getTreeById(treeId)
        treeViewModel.selectedTree.observe(this) { tree ->
            if (tree != null) {
                Glide.with(this).load(tree.imageUrl).into(binding.treeImage)
                binding.toolbar.title = tree.commonName
                binding.commonName.text = tree.commonName
                binding.scientificName.text = tree.scientificName
                binding.co2Saving.text = "${tree.averageCO2SavingKg} kg"
                binding.locationText.text = "${tree.city}, ${tree.state}, ${tree.country}"
                binding.descriptionText.text = tree.description
                binding.nativeRegionText.text = tree.nativeRegion
                binding.plantedOn.text = "Planted on ${
                    UtilityFunctions.formatDate(
                        tree.timestamp.toDate(),
                        "MMM dd, yyyy hh:mm a"
                    )
                }"

                binding.verificationStatus.text =
                    if (tree.isVerified) "Verified by AI" else "Under Verification"
                binding.verificationBadge.imageTintList = ColorStateList.valueOf(
                    if (tree.isVerified) getColor(R.color.primary) else getColor(R.color.warning_text_color)
                )
                binding.treeAge.text = UtilityFunctions.calculateAge(tree.timestamp.toDate())
                binding.mapDirection.setOnClickListener {
                    if (tree.latitude !== null && tree.longitude !== null) {
                        UtilityFunctions.openMap(tree.latitude!!, tree.longitude!!, this)
                    }
                }
            }
        }
    }
}