package `in`.qwicklabs.vanam.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import `in`.qwicklabs.vanam.adapter.PostsAdapter
import `in`.qwicklabs.vanam.databinding.BottomsheetCreatePostBinding
import `in`.qwicklabs.vanam.databinding.FragmentCommunityBinding
import `in`.qwicklabs.vanam.model.PostItem
import `in`.qwicklabs.vanam.repository.FirebaseRepository
import `in`.qwicklabs.vanam.utils.UtilityFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class Community : Fragment() {
    private lateinit var binding: FragmentCommunityBinding
    private lateinit var adapter: PostsAdapter

    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var createPostBinding: BottomsheetCreatePostBinding
    private lateinit var browseImageLauncher: ActivityResultLauncher<String>

    private var uploadedImage: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        browseImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){uri->
            uri?.let {
                createPostBinding.imagePreview.setImageURI(it)
                uploadedImage = it
                createPostBinding.imagePreviewContent.visibility = View.VISIBLE
                createPostBinding.imagePreviewBrowseText.text = "Change Image"
            }
        }

        setupRecycler()
        setUpCreatePost()

        lifecycleScope.launch {
            loadPosts()
        }

        binding.apply {
            toolbar.setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }

            newPost.setOnClickListener {
                bottomSheetDialog.show()
            }

            search.addTextChangedListener(object: TextWatcher{
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    val newText = s.toString()
                    adapter.searchPosts(newText)
                }
            })
        }
    }

    private fun setupRecycler() {
        adapter = PostsAdapter(mutableListOf())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.setHasFixedSize(true)
    }

    private suspend fun loadPosts() {
        val posts = FirebaseRepository.getPostCollection().get().await()
        val postItems = posts.mapNotNull { it.toObject(PostItem::class.java) }

        adapter.updatePostItems(postItems)
    }

    private fun setUpCreatePost(){
        bottomSheetDialog = BottomSheetDialog(requireContext())
        createPostBinding = BottomsheetCreatePostBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(createPostBinding.root)

        createPostBinding.imagePreviewBrowse.setOnClickListener {
            browseImageLauncher.launch("image/*")
        }

        createPostBinding.imagePreviewClear.setOnClickListener {
            resetDialogImage()
        }

        createPostBinding.close.setOnClickListener {
            bottomSheetDialog.dismiss()
            resetDialogImage()
            createPostBinding.content.text.clear()
        }

        createPostBinding.publish.setOnClickListener {
            val content = createPostBinding.content.text.toString()

            if(content.isEmpty()){
                createPostBinding.content.error = "Content cannot be empty"
                return@setOnClickListener
            }

            publishPost(content)
        }
    }

    private fun showLoader(status: Boolean){
        createPostBinding.close.isEnabled = !status
        createPostBinding.publish.isEnabled = !status
        createPostBinding.imagePreviewBrowse.isEnabled  = !status
        createPostBinding.imagePreviewClear.isEnabled = !status
        createPostBinding.content.isEnabled = !status

        createPostBinding.publish.text = if(status) "Publishing..." else "Publish"
    }

    private fun resetDialogImage(){
        createPostBinding.imagePreview.setImageDrawable(null)
        createPostBinding.imagePreviewContent.visibility = View.GONE
        createPostBinding.imagePreviewBrowseText.text = "Upload Image"
        uploadedImage = null
    }

    private suspend fun uploadImageToFirebase(postItem: PostItem): String {
        return withContext(Dispatchers.IO) {
            if (uploadedImage == null) {
                throw IllegalArgumentException("No image selected")
            }

            val imageRef = FirebaseRepository.getPostImageRef(postItem.id)

            try {
                imageRef.putFile(uploadedImage!!).await()

                val downloadUrl = imageRef.downloadUrl.await()

                downloadUrl.toString()
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private fun publishPost(content: String){
        showLoader(true)

        val userId = FirebaseRepository.getCurrentUserId()

        val postItem = PostItem(
            id = UUID.randomUUID().toString(),
            userId = userId,
            content = content,
        )

        lifecycleScope.launch {
            try{
                if(uploadedImage !== null){
                    val downloadableUrl = uploadImageToFirebase(postItem)
                    postItem.imageUrl = downloadableUrl
                }

                val postDocument = FirebaseRepository.getPostCollection().document(postItem.id)
                postDocument.set(postItem).await()

                loadPosts()
                bottomSheetDialog.dismiss()
                resetDialogImage()
                createPostBinding.content.text.clear()
                showLoader(false)
                Toast.makeText(requireContext(), "Post published successfully", Toast.LENGTH_SHORT).show()
            }catch (e: Exception){
                showLoader(false)
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}