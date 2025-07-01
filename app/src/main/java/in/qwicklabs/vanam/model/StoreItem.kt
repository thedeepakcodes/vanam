package `in`.qwicklabs.vanam.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StoreItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Int = 0,
    val image: String = "",
    val category: StoreCategory = StoreCategory.PRODUCTS
) : Parcelable

enum class StoreCategory {
    PRODUCTS,
    COUPONS,
    SAPLING
}