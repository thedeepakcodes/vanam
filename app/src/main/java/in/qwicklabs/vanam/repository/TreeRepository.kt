package `in`.qwicklabs.vanam.repository

import `in`.qwicklabs.vanam.model.Tree
import kotlinx.coroutines.tasks.await

object TreeRepository {
    suspend fun addTree(treeId: String, tree: Tree) {
        val globalTreeDoc = FirebaseRepository.getGlobalTreesCollection().document(treeId)
        globalTreeDoc.set(tree).await()
    }

    suspend fun getMyTrees(): List<Tree> {
        val treeSnapshots = FirebaseRepository.getGlobalTreesCollection()
            .whereEqualTo("userId", FirebaseRepository.getCurrentUserId()).get().await()

        return treeSnapshots.documents.mapNotNull { it.toObject(Tree::class.java) }
            .sortedByDescending { it.timestamp }
    }

    suspend fun getTreeById(treeId: String): Tree? {
        val globalTreeDoc =
            FirebaseRepository.getGlobalTreesCollection().document(treeId).get().await()

        return globalTreeDoc.toObject(Tree::class.java)
    }
}