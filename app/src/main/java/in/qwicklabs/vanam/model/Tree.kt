package `in`.qwicklabs.vanam.model

data class Tree(
    val photoUrl: String,
    val plantedAt: Long,
    val plantName: String,
    val plantType: String,
    val plantId: String,
    val lat: Double,
    val lon: Double,
    val userId: String,
    val verified: Boolean,
)