package `in`.qwicklabs.vanam.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import `in`.qwicklabs.vanam.databinding.ItemPostBinding
import `in`.qwicklabs.vanam.model.PostItem
import `in`.qwicklabs.vanam.repository.FirebaseRepository
import `in`.qwicklabs.vanam.repository.UserRepository
import `in`.qwicklabs.vanam.utils.UtilityFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PostsAdapter(private val postItems: MutableList<PostItem>): RecyclerView.Adapter<PostsAdapter.PostsAdapterViewHolder>(){
    val viewScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val filteredPostItems = mutableListOf<PostItem>()

    init {
        filteredPostItems.addAll(postItems)
    }

    fun updatePostItems(newPostItems: List<PostItem>){
        postItems.clear()
        postItems.addAll(newPostItems.sortedByDescending { it.timestamp })
        filteredPostItems.clear()
        filteredPostItems.addAll(postItems)

        notifyDataSetChanged()
    }

    fun searchPosts(query: String) {
        val lowerQuery = query.lowercase()

        filteredPostItems.clear()

        if (lowerQuery.isEmpty()) {
            filteredPostItems.addAll(postItems)
        } else {
            filteredPostItems.addAll(postItems.filter {
                (it.content.contains(lowerQuery, true) || it.timestamp.toDate().toString().contains(lowerQuery, true))
            })
        }

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PostsAdapter.PostsAdapterViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val postItemView = ItemPostBinding.inflate(inflater, parent, false)

        return PostsAdapterViewHolder(postItemView)
    }

    override fun onBindViewHolder(holder: PostsAdapter.PostsAdapterViewHolder, position: Int) {
        val postItem = filteredPostItems[position]
        return holder.bind(postItem)
    }

    override fun getItemCount(): Int = filteredPostItems.count()

    inner class PostsAdapterViewHolder(val binding: ItemPostBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(postItem: PostItem){
            binding.postContent.text = postItem.content
            binding.postTime.text = UtilityFunctions.formatDate(postItem.timestamp.toDate(), "dd MMM yyyy")
            binding.postImage.visibility = if(postItem.imageUrl != null) View.VISIBLE else View.GONE

            Glide.with(binding.root.context).load(postItem.imageUrl).into(binding.postImage)
            val currentUserId = FirebaseRepository.getCurrentUserId()
            binding.deletePost.visibility = if(currentUserId == postItem.userId) View.VISIBLE else View.GONE

            viewScope.launch {
                val user = UserRepository.getUserById(postItem.userId)

                if(user !== null){
                    binding.userName.text = user.name
                    Glide.with(binding.root.context).load(user.photoUrl).circleCrop().into(binding.userImage)
                }
            }

            if(currentUserId == postItem.userId){
                binding.deletePost.setOnClickListener {
                    MaterialAlertDialogBuilder(binding.root.context)
                        .setTitle("Delete Post")
                        .setMessage("Are you sure you want to delete this post?")
                        .setCancelable(false)
                        .setPositiveButton("Delete"){ dialog, _ ->
                            viewScope.launch {
                                val postDoc = FirebaseRepository.getPostCollection().document(postItem.id)
                                postDoc.delete().await()

                                if(postItem.imageUrl !== null){
                                    val postImageRef = FirebaseRepository.getPostImageRef(postItem.id)
                                    postImageRef.delete().await()
                                }

                                Toast.makeText(binding.root.context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                                updatePostItems(postItems.filter { it.id !== postItem.id })
                            }
                        }
                        .setNegativeButton("Cancel"){
                            dialog, _ -> dialog.dismiss()
                        }.show()
                }
            }
        }
    }
}