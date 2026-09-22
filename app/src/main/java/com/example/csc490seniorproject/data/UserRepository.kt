package com.example.csc490seniorproject.data

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

data class UserProfile(
val username: String ="",
val email: String = "",
val bio: String ="",
val profilePhotoURL: String ="",
val createdAt: Long = System.currentTimeMillis(),
val followerCount: Int =0,
val followingCount: Int=0

)

fun createUserProfile(
    firebaseUser: FirebaseUser,
    username: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) ->Unit
){
    val db = FirebaseFirestore.getInstance()
    val profile = UserProfile(
        username =username,
        email = firebaseUser.email ?: ""
    )

    db.collection("users")
        .document(firebaseUser.uid)
        .set(profile)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e-> onFailure(e) }
}
fun getUserProfile(
    uid: String,
    onSuccess: (UserProfile) -> Unit,
    onFailure: (Exception) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("users")
        .document(uid)
        .get()
        .addOnSuccessListener { snapshot ->
            val profile = snapshot.toObject(UserProfile::class.java)
            if (profile != null) {
                onSuccess(profile)
            } else {
                onFailure(Exception("Profile not found"))
            }
        }
        .addOnFailureListener { e -> onFailure(e) }
}

