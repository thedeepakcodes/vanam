package `in`.qwicklabs.vanam.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import `in`.qwicklabs.vanam.adapter.TreeAdapter
import `in`.qwicklabs.vanam.databinding.ActivityMyTreesBinding
import `in`.qwicklabs.vanam.model.Tree
import `in`.qwicklabs.vanam.viewModel.TreeViewModel

class MyTrees : AppCompatActivity() {
    private lateinit var binding: ActivityMyTreesBinding
    private val treeViewModel: TreeViewModel by viewModels()
    private lateinit var treeAdapter: TreeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMyTreesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Register Activity Result Launcher for adding a new tree
        val addTreeForResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val newTree = result.data?.getParcelableExtra<Tree>("new_tree")

                    newTree?.let {
                        treeAdapter.addTree(it)
                        binding.recyclerView.smoothScrollToPosition(0)
                    }
                }
            }

        loadTrees()
        setupUI()

        // Setup Click Listeners
        binding.apply {
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            fabAdd.setOnClickListener {
                val intent = Intent(this@MyTrees, UploadTree::class.java)
                addTreeForResultLauncher.launch(intent)
            }

            search.addTextChangedListener {
                treeAdapter.search(it.toString())
            }
        }
    }

    private fun setupUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private fun loadTrees() {
        treeViewModel.getMyTrees()
        treeViewModel.trees.observe(this) { trees ->

            if (trees.isEmpty()) {
                binding.recyclerView.visibility = View.GONE
                binding.noTreesPlanted.visibility = View.VISIBLE
            } else {
                binding.recyclerView.visibility = View.VISIBLE
                binding.noTreesPlanted.visibility = View.GONE

                treeAdapter = TreeAdapter(trees.toMutableList())
                binding.recyclerView.adapter = treeAdapter
                binding.recyclerView.layoutManager = LinearLayoutManager(this)
            }
        }
    }
}