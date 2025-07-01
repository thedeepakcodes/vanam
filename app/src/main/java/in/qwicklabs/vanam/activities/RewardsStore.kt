package `in`.qwicklabs.vanam.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.adapter.StoreItemAdapter
import `in`.qwicklabs.vanam.databinding.ActivityRewardsStoreBinding
import `in`.qwicklabs.vanam.model.StoreCategory
import `in`.qwicklabs.vanam.repository.StoreRepository
import kotlinx.coroutines.launch

class RewardsStore : AppCompatActivity() {
    private lateinit var binding: ActivityRewardsStoreBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRewardsStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadItems(null);

        binding.apply {
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            btnAll.setOnClickListener {
                loadItems(null)
            }

            btnProducts.setOnClickListener {
                loadItems(StoreCategory.PRODUCTS)
            }

            btnCoupons.setOnClickListener {
                loadItems(StoreCategory.COUPONS)
            }

            btnSapling.setOnClickListener {
                loadItems(StoreCategory.SAPLING)
            }
        }

        setupUI()
    }

    private fun loadItems(category: StoreCategory?) {
        highlightCategory(category)

        lifecycleScope.launch {
            val items = StoreRepository().getStoreItems(category)
            binding.recyclerView.adapter = StoreItemAdapter(items)
            binding.recyclerView.setHasFixedSize(true)
            binding.recyclerView.layoutManager = GridLayoutManager(this@RewardsStore, 2)
        }
    }

    private fun highlightCategory(category: StoreCategory?) {
        val categories = listOf(binding.btnProducts, binding.btnCoupons, binding.btnSapling)

        categories.forEach { btn ->
            if (btn.text.toString().uppercase() == category?.toString()) {
                btn.setBackgroundColor(resources.getColor(R.color.primary))
                btn.setTextColor(resources.getColor(R.color.white))
            } else {
                btn.setBackgroundColor(resources.getColor(R.color.white))
                btn.setTextColor(resources.getColor(R.color.black))
            }
        }

        if (category == null) {
            binding.btnAll.setBackgroundColor(resources.getColor(R.color.primary))
            binding.btnAll.setTextColor(resources.getColor(R.color.white))
        } else {
            binding.btnAll.setBackgroundColor(resources.getColor(R.color.white))
            binding.btnAll.setTextColor(resources.getColor(R.color.black))
        }
    }

    private fun setupUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}