package com.example.csc490seniorproject.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.csc490seniorproject.objects.Song
import com.example.csc490seniorproject.objects.User
import kotlinx.coroutines.launch
import java.util.ArrayList

class ProfileScreenVM(application: Application) : AndroidViewModel(application) {
    var songsList = ArrayList<Song>()
    var friendsList = ArrayList<User>()
    var messageList = ArrayList<String>()

    init {
        viewModelScope.launch {
            songsList.add(Song(0, "Placeholder", 2.9))
            songsList.add(Song(0, "Placeholder2", 4.9))
            friendsList.add(User("Username", "Fname", "Lname", false,
                "1/1/2020", "email@mmm", songsList, friendsList))
            friendsList.add(User("Username2", "Fname", "Lname", false,
                "1/1/2020", "email@mmm", songsList, friendsList))
            messageList.add("Hey")
        }
    }
}