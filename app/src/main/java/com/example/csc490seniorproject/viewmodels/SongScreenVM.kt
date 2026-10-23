package com.example.csc490seniorproject.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class SongScreenVM(application: Application) : AndroidViewModel(application) {
    fun saveSongRating(
        songId: String,
        ratingValue: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            onError("Please log in to rate this song.")
            return
        }

        if (songId.isBlank() || ratingValue !in 1..5) {
            onError("Rating must be between 1 and 5.")
            return
        }

        val ratingRef = db.collection("songs")
            .document(songId)
            .collection("ratings")
            .document(userId)

        val ratingData = mapOf(
            "userId" to userId,
            "rating" to ratingValue,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        ratingRef.set(ratingData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Unable to save rating.")
            }
    }
}