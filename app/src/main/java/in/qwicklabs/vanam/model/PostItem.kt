package `in`.qwicklabs.vanam.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class PostItem(
    val id: String = "",
    val userId: String = "",
    val content: String = "",
    var imageUrl: String? = null,
    val timestamp: Timestamp = Timestamp.now()
) : Parcelable