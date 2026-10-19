package com.example.csc490seniorproject.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.csc490seniorproject.objects.Song
import com.example.csc490seniorproject.objects.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SearchScreenVM(application: Application) : AndroidViewModel(application) {
    val songsList = mutableStateListOf<Song>()
    val userResults = mutableStateListOf<User>()
    val requestedUids = mutableStateListOf<String>()

    var songsError by mutableStateOf<String?>(null)
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var songsListener: ListenerRegistration? = null
    private var songsGeneration = 0
    private var searchGeneration = 0

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        songsGeneration++
        val generation = songsGeneration

        songsListener?.remove()
        songsListener = null

        songsList.clear()
        userResults.clear()
        requestedUids.clear()
        songsError = null
        searchGeneration++

        if (firebaseAuth.currentUser == null) {
            songsError = "Please log in to see songs."
        } else {
            songsListener = db.collection("songs")
                .addSnapshotListener { snapshot, error ->
                    if (generation != songsGeneration) {
                        return@addSnapshotListener
                    }

                    if (error != null) {
                        songsError = error.localizedMessage
                            ?: "Unable to load songs."
                        return@addSnapshotListener
                    }

                    try {
                        val songs = snapshot?.documents.orEmpty()
                            .mapNotNull { document ->
                                document.toObject(Song::class.java)
                                    ?.copy(id = document.id)
                            }

                        songsList.clear()
                        songsList.addAll(
                            songs.sortedBy { it.name.lowercase() }
                        )
                        songsError = null
                    } catch (e: Exception) {
                        songsError = e.localizedMessage
                            ?: "A song has an invalid field."
                    }
                }
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun searchUsers(query: String) {
        val lowerQuery = query.trim().lowercase()
        searchGeneration++
        val thisGeneration = searchGeneration

        userResults.clear()
        if (lowerQuery.isEmpty()) return

        db.collection("usernames")
            .orderBy(FieldPath.documentId())
            .startAt(lowerQuery)
            .endAt(lowerQuery + "\uf8ff")
            .limit(20)
            .get()
            .addOnSuccessListener usernamesLoaded@{ snapshot ->
                if (thisGeneration != searchGeneration) {
                    return@usernamesLoaded
                }

                val uids = snapshot.documents
                    .mapNotNull { it.getString("uid") }
                    .distinct()

                for (uid in uids) {
                    db.collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener userLoaded@{ document ->
                            if (thisGeneration != searchGeneration) {
                                return@userLoaded
                            }

                            if (
                                document.exists() &&
                                userResults.none { it.uid == uid }
                            ) {
                                userResults.add(
                                    User(
                                        uid = uid,
                                        username = document.getString("username") ?: "",
                                        first_name = document.getString("first_name") ?: "",
                                        last_name = document.getString("last_name") ?: "",
                                        privacy = document.getBoolean("privacy") ?: false,
                                        DoB = document.getString("dob") ?: "",
                                        email = document.getString("email") ?: "",
                                        songs = arrayListOf(),
                                        friends = arrayListOf()
                                    )
                                )
                            }
                        }
                        .addOnFailureListener { error ->
                            println("FIREBASE: Loading user failed: $error")
                        }
                }
            }
            .addOnFailureListener { error ->
                println("FIREBASE: searchUsers failed: $error")
            }
    }

    fun sendFriendRequest(myUid: String, targetUid: String) {
        if (auth.currentUser?.uid != myUid || myUid == targetUid) return

        val generation = songsGeneration

        db.collection("users")
            .document(targetUid)
            .collection("friendRequests")
            .document(myUid)
            .set(
                mapOf(
                    "from" to myUid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                if (
                    generation == songsGeneration &&
                    auth.currentUser?.uid == myUid &&
                    targetUid !in requestedUids
                ) {
                    requestedUids.add(targetUid)
                }
            }
            .addOnFailureListener { error ->
                println("FIREBASE: sendFriendRequest failed: $error")
            }
    }

    override fun onCleared() {
        songsGeneration++
        searchGeneration++
        auth.removeAuthStateListener(authListener)
        songsListener?.remove()
        super.onCleared()
    }
}
