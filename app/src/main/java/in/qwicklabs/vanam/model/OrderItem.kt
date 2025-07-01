package `in`.qwicklabs.vanam.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OrderItem(
    val id: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val itemImage: String = "",
    val itemPrice: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val shippingAddress: String = "",
    val phoneNumber: String = "",
    val deliveryInstructions: String = "",
    val status: String = "Pending",
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
