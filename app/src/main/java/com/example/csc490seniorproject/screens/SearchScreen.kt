package com.example.csc490seniorproject.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.csc490seniorproject.R
import com.example.csc490seniorproject.viewmodels.SearchScreenVM
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.roundToInt

@Composable
fun SearchScreen(viewModel: SearchScreenVM, navController: NavController) {
    val purple = Color(0xFF5D36EB)
    val cardPurple = Color(0xFFBFB1FA)
    val fredoka = FontFamily(Font(R.font.fredoka_medium, FontWeight.Normal))

    var text by rememberSaveable { mutableStateOf("") }

    val visibleSongs = if (text.isBlank()) {
        emptyList()
    } else {
        viewModel.songsList.filter { song ->
            song.name.contains(text.trim(), ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextField(
            value = text,
            onValueChange = { query ->
                text = query
                viewModel.searchUsers(query)
            },
            label = { Text("Search songs or users") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        viewModel.songsError?.let { error ->
            Text(
                text = "Couldn't load songs: $error",
                color = Color.Red,
                modifier = Modifier.fillMaxWidth()
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = viewModel.userResults,
                key = { "user_${it.uid}" }
            ) { user ->
                Surface(
                    color = cardPurple,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = user.username,
                            fontFamily = fredoka,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

                        if (currentUid != null && currentUid != user.uid) {
                            if (user.uid in viewModel.requestedUids) {
                                OutlinedButton(
                                    onClick = {},
                                    enabled = false
                                ) {
                                    Text("Request Sent")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.sendFriendRequest(
                                            currentUid,
                                            user.uid
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = purple)
                                ) {
                                    Text("Add Friend")
                                }
                            }
                        }
                    }
                }
            }

            if (
                visibleSongs.isEmpty() &&
                viewModel.songsError == null
            ) {
                item {
                    Text(
                        text = if (text.isBlank()) { ""
                        } else {
                            "No matching songs."
                        },
                        color = Color.DarkGray,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            items(
                items = visibleSongs,
                key = { "song_${it.id}" }
            ) { song ->
                Surface(
                    color = cardPurple,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Image(

                                    painter = painterResource(id = R.drawable.default_pfp),


                                    contentDescription = "Song artwork",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(80.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = song.name,
                                    fontFamily = fredoka,
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val stars = song.rating
                                    .coerceIn(0.0, 5.0)
                                    .roundToInt()

                                Text(
                                    text = if (song.rating <= 0.0) {
                                        "No ratings yet"
                                    } else {
                                        "★".repeat(stars) +
                                                "☆".repeat(5 - stars)
                                    },
                                    fontFamily = fredoka,
                                    color = purple
                                )

                                Text(
                                    text = "${song.bpm} BPM",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Playback is not connected yet.
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Listen")
                            }

                            Button(
                                onClick = {
                                    navController.navigate("SongScreen/${android.net.Uri.encode(song.id)}")                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = purple
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "More details",
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}