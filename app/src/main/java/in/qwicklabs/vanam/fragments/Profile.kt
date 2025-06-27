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
import `in`.qwicklabs.vanam.activities.EditProfile
import `in`.qwicklabs.vanam.activities.Settings
import `in`.qwicklabs.vanam.databinding.FragmentProfileBinding
import `in`.qwicklabs.vanam.viewModel.UserViewModel

class Profile : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editProfileBtn.setOnClickListener {
            val intent = Intent(this.context, EditProfile::class.java)
            startActivity(intent)
        }

        binding.btnSettings.setOnClickListener {
            val intent = Intent(this.context, Settings::class.java)
            startActivity(intent)
        }

        observeUserData()
        setupAppBarOffsetListener()
    }

    private fun observeUserData() {
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {

                binding.fullName.text = user.name ?: "Guest"
                binding.username.text = user.username?.let { "@$it" } ?: "@guest"
                binding.bio.text = user.bio ?: ""

                Glide.with(requireContext())
                    .load(user.photoUrl)
                    .placeholder(R.drawable.circular_loader)
                    .error(R.drawable.profile_sample)
                    .circleCrop()
                    .into(binding.profileImage)

                binding.coinCount.text = user.greenCoins.toString()
                binding.toolbarCoinCount.text = user.greenCoins.toString()
                "${user.treesCount} Trees".also { binding.treeCount.text = it }
                "${user.badges.size} Badges".also { binding.badgeCount.text = it }
                "${user.postCount} Posts".also { binding.postsCount.text = it }
            }
        }
    }

    private fun setupAppBarOffsetListener() {
        binding.appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange.toFloat()
            val progress = -verticalOffset / totalScrollRange

            // Set the progress for the MotionLayout
            binding.motionLayout.progress = progress

            // Set the alpha values
            binding.toolbarCoinsInfo.alpha = if (progress > 0.3f) {
                ((progress - 0.3f) * 2f).coerceAtMost(1f)
            } else {
                0f
            }

            binding.username.alpha = 1 - progress * 15f
            binding.coinstv.alpha = 1 - progress * 15f

        }
    }
}
