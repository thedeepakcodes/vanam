package `in`.qwicklabs.vanam.model

data class Tree(
    var id: String = "",
    var userId: String? = null,
    var commonName: String? = null,
    var scientificName: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var description: String? = null,
    var nativeRegion: String? = null,
    var averageCO2SavingKg: Double? = null,
    var isLivePhoto: Boolean = false,
    var imageUrl: String? = null,
    var timestamp: Long? = null,
    var isVerified: Boolean = false,
    var city: String? = null,
    var state: String? = null,
    var country: String? = null
)