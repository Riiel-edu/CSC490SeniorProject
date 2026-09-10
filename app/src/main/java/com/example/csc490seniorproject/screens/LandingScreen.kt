package com.example.csc490seniorproject.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.csc490seniorproject.R
import com.example.csc490seniorproject.viewmodels.LandingScreenVM

@Composable
fun LandingScreen(viewModel: LandingScreenVM, modifier: Modifier) {

    Column() {
        Text("Welcome Back!", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Surface(modifier = Modifier.fillMaxWidth().padding(20.dp), shape = RoundedCornerShape(10.dp), shadowElevation = 30.dp) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Dive right back into the music!")
                Spacer(modifier = Modifier.height(15.dp))
                Box(modifier = Modifier.wrapContentSize(Alignment.Center)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = {

                        }) {
                            Text("Create New Song")
                        }
                        Button(onClick = {

                        }) {
                            Text("Find New Songs")
                        }
                    }
                }

            }
        }
        Surface(modifier = Modifier.fillMaxWidth().padding(20.dp), shape = RoundedCornerShape(10.dp), shadowElevation = 30.dp) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Today's Top 10")
            }
        }

        Row(modifier = Modifier.padding(10.dp)) {
            Surface(shadowElevation = 30.dp, border = BorderStroke(2.dp, Color.Black)) {
                Image(
                    painter = painterResource(id = R.drawable.android_icon),
                    contentScale = ContentScale.Fit,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp)
                )
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Row(modifier = Modifier.padding(10.dp)) {
                    Text("Song name")
                    Text("Rating goes here")
                }
                Surface(shadowElevation = 30.dp) {
                    Text("Description")
                }
            }
        }

        Row() {
            Button(onClick = {

            }) {
                Text("Listen")
            }
            Button(onClick = {

            }) {
                Text("More details")
            }
        }

        Surface(modifier = Modifier.fillMaxWidth().padding(20.dp), shape = RoundedCornerShape(10.dp), shadowElevation = 30.dp) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Friend's Recent Activity")
            }
        }
    }

}