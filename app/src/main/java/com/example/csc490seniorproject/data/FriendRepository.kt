package com.example.csc490seniorproject.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

fun sendFriendRequest(fromUid: String, toUid: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("users").document(toUid)
        .collection("friendRequests").document(fromUid)
        .set(mapOf("from" to fromUid, "createdAt" to FieldValue.serverTimestamp()))
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it) }
}

fun acceptFriendRequest(myUid: String, requesterUid: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val batch = db.batch()
    batch.set(db.collection("users").document(myUid).collection("friends").document(requesterUid), mapOf("createdAt" to FieldValue.serverTimestamp()))
    batch.set(db.collection("users").document(requesterUid).collection("friends").document(myUid), mapOf("createdAt" to FieldValue.serverTimestamp()))
    batch.delete(db.collection("users").document(myUid).collection("friendRequests").document(requesterUid))
    batch.commit().addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailure(it) }
}

fun declineFriendRequest(myUid: String, requesterUid: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("users").document(myUid).collection("friendRequests").document(requesterUid)
        .delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it) }
}
