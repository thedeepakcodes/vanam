package `in`.qwicklabs.vanam.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import `in`.qwicklabs.vanam.EditProfile
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.Settings
import `in`.qwicklabs.vanam.databinding.FragmentProfileBinding

class Profile : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private lateinit var editProfileLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editProfileLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    val name = data?.getStringExtra("name")
                    val username = data?.getStringExtra("username")
                    val bio = data?.getStringExtra("bio")
                    val profileImage = data?.getStringExtra("profileImage")

                    Log.e("TAG", data.toString())
                    binding.fullName.text = name
                    binding.username.text = username
                    binding.bio.text = bio

                    profileImage?.let {
                        Glide.with(this).load(it).circleCrop().into(binding.profileImage)
                    }
                }
            }

        binding.editProfileBtn.setOnClickListener {
            val intent = Intent(this.context, EditProfile::class.java)
            editProfileLauncher.launch(intent)
        }

        binding.btnSettings.setOnClickListener {
            val intent = Intent(this.context, Settings::class.java)
            startActivity(intent)
        }

        fetchUserData()
        setupAppBarOffsetListener()
    }

    private fun fetchUserData() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("Vanam").document("Users").collection("Profile").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val name = doc.getString("name")
                    val username = doc.getString("username")
                    val bio = doc.getString("bio")
                    val coins = doc.getString("greenCoins") ?: 0
                    val trees = doc.getString("treesPlanted") ?: 0
                    val badges = doc.getString("badges") ?: 0
                    val posts = doc.getString("posts") ?: 0

                    val profilePic =
                        doc.getString("profileImage") ?: auth.currentUser?.photoUrl.toString()

                    view?.context?.let {
                        Glide.with(it).load(profilePic).placeholder(R.drawable.circular_loader)
                            .error(R.drawable.profile_sample).circleCrop()
                            .into(binding.profileImage)
                    }

                    binding.fullName.text = when {
                        !name.isNullOrBlank() -> name
                        !auth.currentUser?.displayName.isNullOrBlank() -> auth.currentUser?.displayName
                        else -> "Guest"
                    }

                    binding.username.text = when {
                        !username.isNullOrBlank() -> "@$username"
                        else -> "@guest"
                    }

                    binding.bio.text = bio
                    binding.coinCount.text = coins.toString()
                    binding.toolbarCoinCount.text = coins.toString()
                    binding.treeCount.text = "$trees Trees"
                    binding.badgeCount.text = "$badges Badges"
                    binding.postsCount.text = "$posts Posts"
                } else {
                    Log.w("ProfileFragment", "No user profile found.")
                }
            }.addOnFailureListener { e ->
                Log.e("ProfileFragment", "Failed to fetch profile data", e)
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
