package com.example.csc490seniorproject.data

import com.example.csc490seniorproject.objects.Song
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

fun saveSong(uid: String, song: Song, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
    val songs = FirebaseFirestore.getInstance().collection("songs")
    val doc = if (song.id.isBlank()) songs.document() else songs.document(song.id)
    doc.set(song.copy(id = doc.id, ownerId = uid))
        .addOnSuccessListener { onSuccess(doc.id) }
        .addOnFailureListener { onFailure(it) }
}

fun loadSong(songId: String, onSuccess: (Song?) -> Unit, onFailure: (Exception) -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("songs").document(songId)
        .get()
        .addOnSuccessListener { onSuccess(it.toObject(Song::class.java)) }
        .addOnFailureListener { onFailure(it) }
}

fun loadMySongs(uid: String, onSuccess: (List<Song>) -> Unit, onFailure: (Exception) -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("songs")
        .whereEqualTo("ownerUid", uid)
        .get()
        .addOnSuccessListener { onSuccess(it.toObjects(Song::class.java)) }
        .addOnFailureListener { onFailure(it) }
}

fun loadSongsSharedWithMe(uid: String, onSuccess: (List<Song>) -> Unit, onFailure: (Exception) -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("songs")
        .whereArrayContains("sharedWith", uid)
        .get()
        .addOnSuccessListener { onSuccess(it.toObjects(Song::class.java)) }
        .addOnFailureListener { onFailure(it) }
}

fun shareSong(songId: String, friendUid: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("songs").document(songId)
        .update("sharedWith", FieldValue.arrayUnion(friendUid))
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it) }
}

fun unshareSong(songId: String, friendUid: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("songs").document(songId)
        .update("sharedWith", FieldValue.arrayRemove(friendUid))
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it) }
}

fun deleteSong(songId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("songs").document(songId)
        .delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it) }
}