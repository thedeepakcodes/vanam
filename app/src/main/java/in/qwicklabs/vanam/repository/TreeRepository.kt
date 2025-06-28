package `in`.qwicklabs.vanam.repository

import `in`.qwicklabs.vanam.model.Tree
import kotlinx.coroutines.tasks.await

object TreeRepository {
    suspend fun addTree(treeId: String, tree: Tree) {
        val batch = FirebaseRepository.getFirestoreInstance().batch()

        val userTreeDoc = FirebaseRepository.getUserTreesCollection().document(treeId)
        val globalTreeDoc = FirebaseRepository.getGlobalTreesCollection().document(treeId)

        batch.set(userTreeDoc, tree)
        batch.set(globalTreeDoc, tree)

        batch.commit().await()
    }
}