package `in`.qwicklabs.vanam.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import `in`.qwicklabs.vanam.activities.ProductPage
import `in`.qwicklabs.vanam.databinding.ListStoreItemBinding
import `in`.qwicklabs.vanam.model.StoreItem
import `in`.qwicklabs.vanam.utils.UtilityFunctions

class StoreItemAdapter(private val items: List<StoreItem>) :
    RecyclerView.Adapter<StoreItemAdapter.StoreItemViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StoreItemAdapter.StoreItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val storeItemBinding = ListStoreItemBinding.inflate(inflater, parent, false)

        return StoreItemViewHolder(storeItemBinding)
    }

    override fun onBindViewHolder(holder: StoreItemAdapter.StoreItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.count()

    inner class StoreItemViewHolder(private val binding: ListStoreItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StoreItem) {
            binding.itemTitle.text = item.name
            binding.itemSubtitle.text = item.description

            Glide.with(binding.root.context).load(item.image).into(binding.itemImage)

            binding.itemPrice.text = UtilityFunctions.formatNumberShort(item.price.toString())

            binding.itemRedeemBtn.setOnClickListener {
                val storeItem = items[adapterPosition]

                val context = binding.root.context

                val intent = Intent(context, ProductPage::class.java)
                intent.putExtra("item", storeItem)
                context.startActivity(intent)

            }
        }
    }
}