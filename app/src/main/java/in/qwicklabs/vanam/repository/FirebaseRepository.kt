package `in`.qwicklabs.vanam.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

object FirebaseRepository {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    // User Authentication
    fun getAuthInstance(): FirebaseAuth = auth

    // Firestore
    fun getFirestoreInstance(): FirebaseFirestore = firestore

    // Get current logged-in user ID
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    // User document reference
    fun getUserDocRef(): DocumentReference {
        return firestore.collection("v_Users").document(getCurrentUserId())
    }

    // Tree document reference
    fun getTreeDocRef(treeId: String): DocumentReference {
        return firestore.collection("v_Trees").document(treeId)
    }

    fun getUserTreeDocRef(treeId: String): DocumentReference {
        return getUserDocRef().collection("trees").document(treeId)
    }

    // Subcollection of trees under user
    fun getUserTreesCollection(): CollectionReference {
        return getUserDocRef().collection("trees")
    }

    // Global trees collection (for feed)
    fun getGlobalTreesCollection(): CollectionReference {
        return firestore.collection("v_Trees")
    }

    // Tree image storage reference
    fun getTreeImageStorageRef(treeId: String): StorageReference {
        return storage.reference.child("Vanam/trees/${getCurrentUserId()}/$treeId/")
    }

    // User profile image storage reference
    fun getUserProfileImageRef(): StorageReference {
        return storage.reference.child("Vanam/profiles/${getCurrentUserId()}.jpg")
    }
}
