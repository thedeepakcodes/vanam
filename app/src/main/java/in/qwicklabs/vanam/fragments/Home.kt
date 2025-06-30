package `in`.qwicklabs.vanam.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.activities.LeaderBoard
import `in`.qwicklabs.vanam.activities.MyTrees
import `in`.qwicklabs.vanam.activities.UploadTree
import `in`.qwicklabs.vanam.databinding.FragmentHomeBinding
import `in`.qwicklabs.vanam.utils.UtilityFunctions
import `in`.qwicklabs.vanam.viewModel.UserViewModel
import java.util.Calendar

class Home : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvGreeting.text = getGreetingMessage()

        observeUserData()

        // Handle Click Listeners
        binding.apply {
            addTree.setOnClickListener {
                startActivity(Intent(view.context, UploadTree::class.java))
            }

            myTrees.setOnClickListener {
                startActivity(Intent(view.context, MyTrees::class.java))
            }

            leaderboard.setOnClickListener {
                startActivity(Intent(view.context, LeaderBoard::class.java))
            }
        }
    }

    private fun observeUserData() {
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                val firstName = user.name?.split(" ")?.firstOrNull() ?: "Guest"

                Glide.with(requireContext()).load(user.photoUrl)
                    .placeholder(R.drawable.circular_loader).error(R.drawable.profile_sample)
                    .circleCrop().into(binding.ivProfilePicture)

                binding.tvUserFirstName.text = firstName
                binding.tvTreesPlantedCount.text =
                    UtilityFunctions.formatNumberShort(user.treesCount.toString())
                binding.tvDayStreakCount.text = user.dayStreak.toString()
                binding.tvGreenCoinsCount.text =
                    UtilityFunctions.formatNumberShort(user.greenCoins.toString())
            } else {
                binding.tvUserFirstName.text = "Guest"
            }
        }
    }

    private fun getGreetingMessage(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }
}
