package `in`.qwicklabs.vanam.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.adapter.LeaderboardAdapter
import `in`.qwicklabs.vanam.adapter.LeaderboardList
import `in`.qwicklabs.vanam.databinding.ActivityLeaderBoardBinding
import `in`.qwicklabs.vanam.model.User
import `in`.qwicklabs.vanam.viewModel.UserViewModel

class LeaderBoard : AppCompatActivity() {
    private lateinit var binding: ActivityLeaderBoardBinding
    private val userViewModel: UserViewModel by viewModels()
    private lateinit var adapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        loadUsers("trees")

        binding.apply {
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            sortByTrees.setOnClickListener {
                highlightTab("trees")
                loadUsers("trees")
            }

            sortByCoins.setOnClickListener {
                highlightTab("coins")
                loadUsers("coins")
            }
        }
    }

    private fun setupUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private fun updateLeaderboardWinners(users: List<User>) {
        fun updateWinnerUI(
            winner: User?,
            nameTextView: TextView,
            profileImageView: ImageView,
            root: LinearLayout
        ) {
            winner.let {
                if (winner == null) {
                    root.visibility = View.GONE
                } else {
                    root.visibility = View.VISIBLE
                    nameTextView.text = it?.name
                    Glide.with(this).load(it?.photoUrl?.toUri()).circleCrop().into(profileImageView)
                }
            }
        }

        updateWinnerUI(
            users.getOrNull(0),
            binding.firstWinnerName,
            binding.firstWinnerProfile,
            binding.firstWinnerLl
        )
        updateWinnerUI(
            users.getOrNull(1),
            binding.secondWinnerName,
            binding.secondWinnerImage,
            binding.secondWinnerLl
        )
        updateWinnerUI(
            users.getOrNull(2),
            binding.thirdWinnerName,
            binding.thirdWinnerImage,
            binding.thirdWinnerLl
        )
    }

    private fun loadUsers(sortBy: String) {

        if (sortBy == "trees") {
            userViewModel.getUsersByTreesCount()
        } else {
            userViewModel.getUsersByGreenCoins()
        }

        userViewModel.getUser()

        userViewModel.user.observe(this) { currentUser ->
            userViewModel.users.observe(this) { users ->
                val sortedUsers = users.map {
                    if (sortBy == "trees") LeaderboardList.TreePlantedList(it) else LeaderboardList.GreenCoinList(
                        it
                    )
                }


                val userPos = users.indexOf(currentUser)
                val userRank = userPos + 1

                Glide.with(this).load(currentUser?.photoUrl).circleCrop().into(binding.myPhoto)
                binding.myRank.text = "Your Rank: #$userRank"

                val myProgress =
                    if (sortBy == "trees") currentUser?.treesCount else currentUser?.greenCoins
                val totalProgress =
                    if (sortBy == "trees") users[0].treesCount else users[0].greenCoins

                if (myProgress != null && totalProgress > 0) {
                    val progressPercentage =
                        (myProgress.toDouble() / totalProgress.toDouble()) * 100
                    binding.myRankProgress.progress = progressPercentage.toInt().coerceIn(0, 100)
                } else {
                    binding.myRankProgress.progress = 0
                }
                
                binding.myRankText.text = when (sortBy) {
                    "trees" -> "You're doing amazing! Only ${users[0].treesCount - currentUser.treesCount} more trees to reach the top!"
                    "coins" -> "Great job! Just ${users[0].greenCoins - currentUser.greenCoins} more green coins to become #1!"
                    else -> ""
                }

                if (userRank == 1) {
                    binding.myRankText.text =
                        "You're leading the way! #1 on the leaderboard – thank you for making a difference!"
                }

                updateLeaderboardWinners(users)
                adapter.updateList(sortedUsers)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = LeaderboardAdapter(mutableListOf())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.setHasFixedSize(true)
    }

    private fun highlightTab(tab: String) {
        val focusedTab = when (tab) {
            "trees" -> binding.sortByTrees
            "coins" -> binding.sortByCoins
            else -> null
        }

        val unfocusedTab = when (tab) {
            "trees" -> binding.sortByCoins
            "coins" -> binding.sortByTrees
            else -> null
        }

        focusedTab?.setBackgroundColor(resources.getColor(R.color.primary))
        focusedTab?.setTextColor(resources.getColor(R.color.white))
        unfocusedTab?.setBackgroundColor(resources.getColor(R.color.white))
        unfocusedTab?.setTextColor(resources.getColor(R.color.black))
    }
}