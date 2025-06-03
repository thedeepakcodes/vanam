package `in`.qwicklabs.vanam.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.databinding.FragmentHomeBinding
import java.util.Calendar

class Home : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val userId: String? get() = auth.currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvGreeting.text = getGreetingMessage()

        if (auth.currentUser != null) {
            fetchUserData()
        } else {
            showGuestUser()
        }
    }

    private fun fetchUserData() {
        val uid = userId ?: return showGuestUser()

        firestore.collection("Vanam")
            .document("Users")
            .collection("Profile")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name")
                    val profilePic = document.getString("profileImage")
                        ?: auth.currentUser?.photoUrl.toString()

                    val firstName = name?.takeIf { it.isNotBlank() }
                        ?: auth.currentUser?.displayName?.split(" ")?.firstOrNull()
                        ?: "Guest"

                    binding.tvUserFirstName.text = firstName

                    Glide.with(requireContext())
                        .load(profilePic)
                        .placeholder(R.drawable.circular_loader)
                        .error(R.drawable.profile_sample)
                        .circleCrop()
                        .into(binding.ivProfilePicture)
                } else {
                    showGuestUser()
                    Log.w("HomeFragment", "User profile document does not exist.")
                }
            }
            .addOnFailureListener { e ->
                showGuestUser()
                Log.e("HomeFragment", "Failed to fetch user data", e)
            }
    }

    private fun showGuestUser() {
        binding.tvUserFirstName.text = "Guest"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
