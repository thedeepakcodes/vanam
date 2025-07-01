package `in`.qwicklabs.vanam.repository

import `in`.qwicklabs.vanam.model.StoreCategory
import `in`.qwicklabs.vanam.model.StoreItem
import kotlinx.coroutines.tasks.await

class StoreRepository {
    suspend fun getStoreItems(category: StoreCategory? = null): List<StoreItem> {
        val query = if (category != null) {
            FirebaseRepository.getStoreCollection().whereEqualTo("category", category.name)
        } else {
            FirebaseRepository.getStoreCollection()
        }

        val snapshot = query.get().await()
        return snapshot.documents.mapNotNull { it.toObject(StoreItem::class.java) }
    }
}