package `in`.qwicklabs.vanam.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.activities.ViewTree
import `in`.qwicklabs.vanam.model.Tree
import `in`.qwicklabs.vanam.utils.UtilityFunctions


class TreeAdapter(private val trees: MutableList<Tree>) :
    RecyclerView.Adapter<TreeAdapter.TreeViewHolder>() {

    private val filteredList = mutableListOf<Tree>()

    init {
        filteredList.addAll(trees)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder {
        val treeView =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_my_tree, parent, false)

        return TreeViewHolder(treeView)
    }

    override fun getItemCount(): Int {
        return filteredList.count()
    }

    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) {
        val tree = filteredList[position]
        holder.commonName.text = tree.commonName
        holder.scientificName.text = tree.scientificName
        holder.locationName.text = "${tree.city} • ${tree.state}"
        holder.verifiedBadge.visibility = if (tree.isVerified) View.VISIBLE else View.GONE
        holder.unverifiedBadge.visibility = if (tree.isVerified) View.GONE else View.VISIBLE

        holder.plantedOn.text =
            UtilityFunctions.formatDate(tree.timestamp.toDate(), "MMM dd, yyyy hh:mm a")
        tree.imageUrl?.let { imageUrl ->
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .into(holder.treeImage)
        }
    }

    fun addTree(it: Tree) {
        filteredList.add(0, it)
        notifyItemInserted(0)
    }

    fun search(query: String) {
        val lowerQuery = query.lowercase()

        filteredList.clear()

        if (lowerQuery.isEmpty()) {
            filteredList.addAll(trees)
        } else {
            filteredList.addAll(trees.filter {
                (it.commonName?.contains(lowerQuery, true) == true ||
                        it.scientificName?.contains(lowerQuery, true) == true
                        || it.city?.contains(lowerQuery, true) == true ||
                        it.state?.contains(lowerQuery, true) == true
                        || it.country?.contains(lowerQuery, true) == true
                        || lowerQuery == "pending" && !it.isVerified
                        || lowerQuery == "verified" && it.isVerified
                        || it.description?.contains(lowerQuery, true) == true)
                        || it.timestamp.toDate().toString().contains(lowerQuery, true)
            })
        }

        notifyDataSetChanged()

    }

    inner class TreeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val commonName: TextView = view.findViewById(R.id.common_name)
        val scientificName: TextView = view.findViewById(R.id.scientific_name)
        val locationName: TextView = view.findViewById(R.id.location_name)
        val plantedOn: TextView = view.findViewById(R.id.planted_on)
        val treeImage: ImageView = view.findViewById(R.id.photo)
        val verifiedBadge: ImageView = view.findViewById(R.id.verified_badge)
        val unverifiedBadge: ImageView = view.findViewById(R.id.unverified_badge)

        init {
            locationName.setOnClickListener {
                val tree = filteredList[adapterPosition]
                if (tree.latitude == null || tree.longitude == null) return@setOnClickListener

                UtilityFunctions.openMap(tree.latitude!!, tree.longitude!!, it.context)
            }

            view.setOnClickListener {
                val tree = filteredList[adapterPosition]
                it.context.startActivity(
                    Intent(
                        it.context,
                        ViewTree::class.java
                    ).apply { putExtra("treeId", tree.id) })
            }
        }
    }
}

