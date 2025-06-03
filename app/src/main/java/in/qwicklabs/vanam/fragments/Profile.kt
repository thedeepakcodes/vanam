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
import `in`.qwicklabs.vanam.databinding.FragmentProfileBinding

class Profile : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchUserData()
    }

    private fun fetchUserData() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("Vanam")
            .document("Users")
            .collection("Profile")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val name = doc.getString("name")
                    val username = doc.getString("username")
                    val profilePic =
                        doc.getString("profileImage") ?: auth.currentUser?.photoUrl.toString()

                    Glide.with(requireContext())
                        .load(profilePic)
                        .placeholder(R.drawable.circular_loader)
                        .error(R.drawable.profile_sample)
                        .circleCrop()
                        .into(binding.profileImage)

                    binding.fullName.text = when {
                        !name.isNullOrBlank() -> name
                        !auth.currentUser?.displayName.isNullOrBlank() -> auth.currentUser?.displayName
                        else -> "Guest"
                    }

                    binding.username.text = when {
                        !username.isNullOrBlank() -> "@$username"
                        else -> "@guest"
                    }
                } else {
                    Log.w("ProfileFragment", "No user profile found.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Failed to fetch profile data", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
