package com.example.csc490seniorproject.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.csc490seniorproject.objects.Song
import com.example.csc490seniorproject.objects.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlin.collections.set
import kotlin.text.get

enum class FriendStatus { NONE, REQUEST_SENT, REQUEST_RECEIVED, FRIENDS }

class ProfileScreenVM(application: Application) : AndroidViewModel(application) {
    val songsList = mutableStateListOf<Song>()
    val friendsList = mutableStateListOf<User>()
    var messageList = mutableStateListOf<String>()

    var profileUid by mutableStateOf<String?>(null)
    var friendStatus by mutableStateOf(FriendStatus.NONE)
    val incomingRequests = mutableStateListOf<User>()
    var profileUsername by mutableStateOf("")
    var aboutMe by mutableStateOf("")
    var profilePictureUrl by mutableStateOf("")
    private val db = FirebaseFirestore.getInstance()

    init {
        viewModelScope.launch {
            songsList.add(Song())
            songsList.add(Song())
            friendsList.add(User(
                username = "Username",
                first_name = "Fname",
                last_name = "Lname",
                privacy = false,
                DoB = "1/1/2020",
                email = "email@mmm",
                songs = songsList,
                friends = friendsList
            ))
            friendsList.add(User(
                username = "Username2",
                first_name = "Fname",
                last_name = "Lname",
                privacy = false,
                DoB = "1/1/2020",
                email = "email@mmm",
                songs = songsList,
                friends = friendsList
            ))
            messageList.add("Hey")

        }
    }

    // Resolves a username to the real Firebase Auth uid, via the same `usernames`
    // lookup collection the login screen already uses.
    private fun resolveUid(username: String, onResolved: (String) -> Unit) {
        db.collection("usernames").document(username.lowercase()).get()
            .addOnSuccessListener { doc ->
                val uid = doc.getString("uid")
                if (uid != null) onResolved(uid)
                else println("FIREBASE: no uid found for username $username")
            }
            .addOnFailureListener { e -> println("FIREBASE: resolveUid failed: $e") }
    }

    // Call once when ProfileScreen loads.
    fun loadProfile(username: String, currentUid: String?) {
        resolveUid(username) { uid ->
            profileUid = uid
            getSongs(uid)
            getFriends(uid)
            if (currentUid != null) {
                if (currentUid == uid) getIncomingRequests(currentUid)
                else checkFriendStatus(currentUid, uid)
            }
        }
    }

    fun getSongs(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    songsList.clear()
                    val songs = document.get("songs") as? List<String> ?: emptyList()
                    for (songId in songs) {
                        db.collection("songs").document(songId).get()
                            .addOnSuccessListener { songDocument ->
                                if (songDocument.exists()) {
                                    songsList.add(Song(
                                        id = songId,
                                        name = songDocument.getString("name") ?: "",
                                        rating = songDocument.getDouble("rating") ?: 0.0
                                    ))
                                }
                            }
                    }
                }
            }
    }

    fun getFriends(uid: String) {
        println("friends: getFriends() called with uid = $uid")
        friendsList.clear()

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    println("friends: User document does not exist")
                    return@addOnSuccessListener
                }

                val friendUids = document.get("friends") as? List<String> ?: emptyList()

                println("friends: Found ${friendUids.size} friend UIDs")
                println("friends: Friend UIDs = $friendUids")

                for (friendUid in friendUids) {
                    println("friends: Loading friend UID = $friendUid")

                    db.collection("users").document(friendUid).get()
                        .addOnSuccessListener { friendDocument ->
                            if (friendDocument.exists()) {
                                val friend = User(
                                    username = friendDocument.getString("username") ?: "",
                                    first_name = friendDocument.getString("first_name") ?: "",
                                    last_name = friendDocument.getString("last_name") ?: "",
                                    privacy = friendDocument.getBoolean("privacy") ?: false,
                                    DoB = friendDocument.getString("dob") ?: "",
                                    email = friendDocument.getString("email") ?: "",
                                    songs = arrayListOf(),
                                    friends = arrayListOf(),
                                    uid = friendUid
                                )

                                friendsList.add(friend)

                                println("friends: Added ${friend.username}")
                                println("friends: Current friendsList size = ${friendsList.size}")
                            } else {
                                println("friends: User document not found for UID = $friendUid")
                            }
                        }
                        .addOnFailureListener { e ->
                            println("friends: Failed to load $friendUid: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                println("friends: Failed to load user's friends: ${e.message}")
            }
    }

    fun checkFriendStatus(myUid: String, otherUid: String) {
        db.collection("users").document(myUid).collection("friends").document(otherUid).get()
            .addOnSuccessListener { friendDoc ->
                if (friendDoc.exists()) { friendStatus = FriendStatus.FRIENDS; return@addOnSuccessListener }
                db.collection("users").document(otherUid).collection("friendRequests").document(myUid).get()
                    .addOnSuccessListener { sentDoc ->
                        if (sentDoc.exists()) { friendStatus = FriendStatus.REQUEST_SENT; return@addOnSuccessListener }
                        db.collection("users").document(myUid).collection("friendRequests").document(otherUid).get()
                            .addOnSuccessListener { receivedDoc ->
                                friendStatus = if (receivedDoc.exists()) FriendStatus.REQUEST_RECEIVED else FriendStatus.NONE
                            }
                    }
            }
    }

    fun sendFriendRequest(myUid: String, targetUid: String) {
        db.collection("users").document(targetUid).collection("friendRequests").document(myUid)
            .set(mapOf("from" to myUid, "createdAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { friendStatus = FriendStatus.REQUEST_SENT }
            .addOnFailureListener { e -> println("FIREBASE: sendFriendRequest failed: $e") }
    }

    fun acceptFriendRequest(myUid: String, requesterUid: String) {
        val batch = db.batch()
        batch.set(db.collection("users").document(myUid).collection("friends").document(requesterUid),
            mapOf("createdAt" to FieldValue.serverTimestamp()))
        batch.set(db.collection("users").document(requesterUid).collection("friends").document(myUid),
            mapOf("createdAt" to FieldValue.serverTimestamp()))
        batch.delete(db.collection("users").document(myUid).collection("friendRequests").document(requesterUid))
        batch.commit()
            .addOnSuccessListener {
                friendStatus = FriendStatus.FRIENDS
                incomingRequests.removeAll { it.uid == requesterUid }
                getFriends(myUid)
            }
            .addOnFailureListener { e -> println("FIREBASE: acceptFriendRequest failed: $e") }
    }

    fun declineFriendRequest(myUid: String, requesterUid: String) {
        db.collection("users").document(myUid).collection("friendRequests").document(requesterUid)
            .delete()
            .addOnSuccessListener {
                incomingRequests.removeAll { it.uid == requesterUid }
                if (friendStatus == FriendStatus.REQUEST_RECEIVED) friendStatus = FriendStatus.NONE
            }
            .addOnFailureListener { e -> println("FIREBASE: declineFriendRequest failed: $e") }
    }

    fun getIncomingRequests(myUid: String) {
        incomingRequests.clear()
        db.collection("users").document(myUid).collection("friendRequests").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val requesterUid = doc.id
                    db.collection("users").document(requesterUid).get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                incomingRequests.add(User(
                                    username = userDoc.getString("username") ?: "",
                                    first_name = userDoc.getString("first_name") ?: "",
                                    last_name = userDoc.getString("last_name") ?: "",
                                    privacy = userDoc.getBoolean("privacy") ?: false,
                                    DoB = userDoc.getString("dob") ?: "",
                                    email = userDoc.getString("email") ?: "",
                                    songs = arrayListOf(),
                                    friends = arrayListOf(),
                                    uid = requesterUid
                                ))
                            }
                        }
                }
            }
    }

    fun getProfileDetails(myUid: String, onLoaded: () -> Unit = {}) {
        db.collection("users").document(myUid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val displayName = document.getString("displayName")
                    val username = document.getString("username") ?: ""

                    // Use displayName if it exists; otherwise use username
                    profileUsername = if (displayName.isNullOrBlank()) {
                        username
                    } else {
                        displayName
                    }

                    aboutMe = document.getString("aboutMe") ?: ""
                    profilePictureUrl = document.getString("profilePictureUrl") ?: ""

                    if (displayName.isNullOrBlank()) {
                        db.collection("users").document(myUid)
                            .set(
                                mapOf("displayName" to username),
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                            .addOnSuccessListener {
                                println("FIREBASE: Created displayName = $username")
                                onLoaded()
                            }
                            .addOnFailureListener { e ->
                                println("FIREBASE: Failed to create displayName: ${e.message}")
                                onLoaded()
                            }
                    } else {
                        println("FIREBASE: Loaded displayName = $displayName")
                        onLoaded()
                    }
                } else {
                    println("FIREBASE: User document does not exist")
                }
            }
            .addOnFailureListener { e ->
                println("FIREBASE: Failed to get profile details: ${e.message}")
            }
    }

    fun updateProfileDetails(myUid: String) {
        val profileData = mapOf(
            "displayName" to profileUsername,
            "aboutMe" to aboutMe,
            "profilePictureUrl" to profilePictureUrl
        )

        db.collection("users").document(myUid)
            .set(profileData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                println("FIREBASE: Profile details updated or created successfully")
            }
            .addOnFailureListener { e ->
                println("FIREBASE: Failed to save profile details: ${e.message}")
            }
    }
}


