package com.example.csc490seniorproject.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.csc490seniorproject.objects.Song
import com.example.csc490seniorproject.objects.User
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.ArrayList
import kotlin.collections.addAll
import kotlin.text.clear


class ProfileScreenVM(application: Application) : AndroidViewModel(application) {
    val songsList = mutableStateListOf<Song>()
    val friendsList = mutableStateListOf<User>()
    var messageList = mutableStateListOf<String>()



    init {
        viewModelScope.launch {
            songsList.add(Song())
            songsList.add(Song())
            friendsList.add(User("Username", "Fname", "Lname", false,
                "1/1/2020", "email@mmm", songsList, friendsList))
            friendsList.add(User("Username2", "Fname", "Lname", false,
                "1/1/2020", "email@mmm", songsList, friendsList))
            messageList.add("Hey")
        }
    }

    fun getSongs(username: String) {
        val app = FirebaseApp.initializeApp(getApplication())

        if (app == null) {
            println("FIREBASE: Initialization failed")
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(username)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val songs = document.get("songs") as? List<String>
                        ?: emptyList()



                    for (songId in songs) {

                        db.collection("songs")
                            .document(songId)
                            .get()
                            .addOnSuccessListener { songDocument ->

                                if (songDocument.exists()) {

                                    val song = Song(
                                        id= songId,
                                        name = songDocument.getString("name") ?: "",
                                        rating = songDocument.getDouble("rating") ?: 0.0
                                    )

                                    songsList.add(song)
                                }
                            }
                    }
                }
            }

    }
    fun getFriends(username: String) {
        val app = FirebaseApp.initializeApp(getApplication())

        if (app == null) {
            println("FIREBASE: Initialization failed")
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(username)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val friends = document.get("friends") as? List<String>
                        ?: emptyList()

                    friendsList.clear()

                    for (friendId in friends) {

                        db.collection("users")
                            .document(friendId)
                            .get()
                            .addOnSuccessListener { friendDocument ->

                                if (friendDocument.exists()) {
                                    println("FRIEND DOCUMENT: ${friendDocument.data}")
                                    val friend = User(
                                        username = friendDocument.getString("username") ?: "",
                                        first_name = friendDocument.getString("first_name") ?: "",
                                        last_name = friendDocument.getString("last_name") ?: "",
                                        privacy = friendDocument.getBoolean("privacy") ?: false,
                                        DoB = friendDocument.getString("dob") ?: "",
                                        email = friendDocument.getString("email") ?: "",
                                        songs = arrayListOf(),
                                        friends = arrayListOf()
                                    )

                                    friendsList.add(friend)
                                }
                            }
                    }
                }
            }
            .addOnFailureListener {
                friendsList.clear()
            }
    }
}