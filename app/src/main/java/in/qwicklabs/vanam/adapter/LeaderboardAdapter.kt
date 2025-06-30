package `in`.qwicklabs.vanam.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import `in`.qwicklabs.vanam.databinding.ItemLeaderboardCoinsBinding
import `in`.qwicklabs.vanam.databinding.ItemLeaderboardTreesBinding
import `in`.qwicklabs.vanam.model.User
import `in`.qwicklabs.vanam.utils.UtilityFunctions
import okhttp3.internal.format

class LeaderboardAdapter(private val lists: MutableList<LeaderboardList>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_TREE_PLANTED = 0
        const val TYPE_GREEN_COINS = 1
    }

    fun updateList(newList: List<LeaderboardList>) {
        lists.clear()
        lists.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (lists[position]) {
            is LeaderboardList.TreePlantedList -> TYPE_TREE_PLANTED
            is LeaderboardList.GreenCoinList -> TYPE_GREEN_COINS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TREE_PLANTED -> TreePlantedViewHolder(
                ItemLeaderboardTreesBinding.inflate(inflater, parent, false)
            )

            TYPE_GREEN_COINS -> GreenCoinsViewHolder(
                ItemLeaderboardCoinsBinding.inflate(inflater, parent, false)
            )

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val list = lists[position]) {
            is LeaderboardList.GreenCoinList -> (holder as GreenCoinsViewHolder).bind(list.user)
            is LeaderboardList.TreePlantedList -> (holder as TreePlantedViewHolder).bind(list.user)
        }
    }

    override fun getItemCount(): Int = lists.count()

    class TreePlantedViewHolder(val binding: ItemLeaderboardTreesBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.textViewRank.text =
                UtilityFunctions.formatNumberShort((adapterPosition + 1).toString())
            binding.textViewUsername.text = user.name
            binding.textViewCountry.text = user.country
            binding.textViewTreeCount.text = format(user.treesCount.toString())
            Glide.with(binding.root.context).load(user.photoUrl).circleCrop()
                .into(binding.imageViewProfile)
        }
    }

    class GreenCoinsViewHolder(val binding: ItemLeaderboardCoinsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.textViewRank.text =
                UtilityFunctions.formatNumberShort((adapterPosition + 1).toString())
            binding.textViewUsername.text = user.name
            binding.textViewCountry.text = user.country
            binding.textViewCoinCount.text =
                UtilityFunctions.formatNumberShort(user.greenCoins.toString())
            Glide.with(binding.root.context).load(user.photoUrl).circleCrop()
                .into(binding.imageViewProfile)
        }
    }
}

sealed class LeaderboardList {
    data class TreePlantedList(val user: User) : LeaderboardList()
    data class GreenCoinList(val user: User) : LeaderboardList()
}