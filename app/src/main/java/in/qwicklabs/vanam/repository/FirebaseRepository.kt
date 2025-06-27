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

    // Global Firestore access
    fun getFirestoreInstance(): FirebaseFirestore = firestore

    // Global Storage access
    fun getStorageInstance(): FirebaseStorage = storage

    // User Authentication
    fun getAuthInstance(): FirebaseAuth = auth

    // Get current logged-in user ID
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    // User document reference
    fun getUserDocRef(): DocumentReference {
        return firestore.collection("v_Users").document(getCurrentUserId())
    }

    // Subcollection of trees under user
    fun getUserTreesCollection(): CollectionReference {
        return getUserDocRef().collection("trees")
    }

    // Global trees collection (for feed)
    fun getGlobalTreesCollection(): CollectionReference {
        return firestore.collection("v_Trees")
    }

    // Community posts collection
    fun getPostsCollection(): CollectionReference {
        return firestore.collection("v_Posts")
    }

    // Likes subcollection under a post
    fun getPostLikesCollection(postId: String): CollectionReference {
        return getPostsCollection().document(postId).collection("likes")
    }

    // Tree image storage reference
    fun getTreeImageStorageRef(treeId: String): StorageReference {
        return storage.reference.child("Vanam/trees/${getCurrentUserId()}/$treeId/")
    }

    // User profile image storage reference
    fun getUserProfileImageRef(): StorageReference {
        return storage.reference.child("Vanam/profiles/${getCurrentUserId()}.jpg")
    }

    // Post image storage reference
    fun getPostImageStorageRef(postId: String): StorageReference {
        return storage.reference.child("Vanam/posts/$postId.jpg")
    }
}
