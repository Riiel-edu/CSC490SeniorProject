package com.example.csc490seniorproject.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.csc490seniorproject.objects.Song
import com.example.csc490seniorproject.objects.User
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.ArrayList

class SearchScreenVM(application: Application) : AndroidViewModel(application) {
    var songsList = ArrayList<Song>()
    val userResults = mutableStateListOf<User>()

    val requestedUids = mutableStateListOf<String>()
    private val db = FirebaseFirestore.getInstance()

    init {
        viewModelScope.launch {
            songsList.add(Song())
            songsList.add(Song())
        }
    }


    private var searchGeneration = 0

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
            .addOnSuccessListener { snapshot ->
                if (thisGeneration != searchGeneration) return@addOnSuccessListener
                val uids = snapshot.documents.mapNotNull { it.getString("uid") }
                for (uid in uids) {
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            if (thisGeneration != searchGeneration) return@addOnSuccessListener
                            if (doc.exists() && userResults.none { it.uid == uid }) {
                                userResults.add(User(
                                    uid = uid,
                                    username = doc.getString("username") ?: "",
                                    first_name = doc.getString("first_name") ?: "",
                                    last_name = doc.getString("last_name") ?: "",
                                    privacy = doc.getBoolean("privacy") ?: false,
                                    DoB = doc.getString("dob") ?: "",
                                    email = doc.getString("email") ?: "",
                                    songs = arrayListOf(),
                                    friends = arrayListOf()
                                ))
                            }
                        }
                }
            }
            .addOnFailureListener { e -> println("FIREBASE: searchUsers failed: $e") }
    }
    fun sendFriendRequest(myUid: String, targetUid: String) {
        db.collection("users").document(targetUid).collection("friendRequests").document(myUid)
            .set(mapOf("from" to myUid, "createdAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { requestedUids.add(targetUid) }
            .addOnFailureListener { e -> println("FIREBASE: sendFriendRequest failed: $e") }
    }
}
