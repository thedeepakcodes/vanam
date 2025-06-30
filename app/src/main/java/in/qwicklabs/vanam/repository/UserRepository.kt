package `in`.qwicklabs.vanam.repository

import com.google.firebase.firestore.ListenerRegistration
import `in`.qwicklabs.vanam.model.User
import kotlinx.coroutines.tasks.await

object UserRepository {

    fun listenToUserData(onUserChanged: (User?) -> Unit): ListenerRegistration {
        return FirebaseRepository.getUserDocRef()
            .addSnapshotListener { snapshot, _ ->
                val user = snapshot?.toObject(User::class.java)
                onUserChanged(user)
            }
    }

    suspend fun updateUser(user: User) {
        FirebaseRepository.getUserDocRef().set(user).await()
    }

    suspend fun getUser(): User? {
        val snapshot = FirebaseRepository.getUserDocRef().get().await()
        return snapshot.toObject(User::class.java)
    }


    suspend fun deleteUser() {
        FirebaseRepository.getUserDocRef().delete().await()
    }

    suspend fun incrementTreeCountAndCoins() {
        val user = getUser()
        if (user != null) {
            val newTreeCount = user.treesCount + 1
            val newCoins = user.greenCoins + 100

            val updatedUser = user.copy(treesCount = newTreeCount, greenCoins = newCoins)
            updateUser(updatedUser)
        }
    }

    suspend fun getUsersByTreesCount(): List<User> {
        val snapshot = FirebaseRepository.getUserCollection()
            .orderBy("treesCount")
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            .sortedByDescending { it.treesCount }
    }

    suspend fun getUsersByGreenCoins(): List<User> {
        val snapshot = FirebaseRepository.getUserCollection()
            .orderBy("greenCoins")
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            .sortedByDescending { it.greenCoins }
    }
}

