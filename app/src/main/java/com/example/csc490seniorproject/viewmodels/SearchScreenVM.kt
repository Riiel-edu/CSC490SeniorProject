package com.example.csc490seniorproject.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.csc490seniorproject.objects.Song
import kotlinx.coroutines.launch
import java.util.ArrayList

class SearchScreenVM(application: Application) : AndroidViewModel(application) {
    var songsList = ArrayList<Song>()

    init {
        viewModelScope.launch {
            songsList.add(Song(0, "Placeholder", 2.9))
            songsList.add(Song(0, "Placeholder2", 4.9))
        }
    }

}