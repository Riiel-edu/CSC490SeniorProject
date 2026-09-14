package com.example.csc490seniorproject.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csc490seniorproject.R
import com.example.csc490seniorproject.viewmodels.LandingScreenVM

@Composable
fun SongScreen(viewModel: LandingScreenVM) {

    val fredoka = FontFamily(Font(R.font.fredoka_medium, FontWeight.Normal))

    Column(modifier = Modifier.fillMaxSize()) {

        Spacer(modifier = Modifier.height(10.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Welcome Back, Username!",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontFamily = fredoka, fontSize = 30.sp,
                color = Color(0xFF5d36eb)
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 30.dp,
            color = Color(0xFFbfb1fa)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "Dive right back into the music!",
                    fontFamily = fredoka,
                    fontSize = 20.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.5.dp))

                HorizontalDivider(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 2.dp
                )

                Spacer(modifier = Modifier.height(7.5.dp))
                Box(modifier = Modifier.wrapContentSize(Alignment.Center)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {

                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5d36eb),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Create New Song", fontFamily = fredoka)
                        }
                        Button(
                            onClick = {

                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5d36eb),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Find New Songs", fontFamily = fredoka)
                        }
                    }
                }

            }
        }
        Surface(
            color = Color(0xFFbfb1fa),
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 30.dp
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "Today's Top 10",
                    fontFamily = fredoka,
                    fontSize = 26.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.5.dp))

                HorizontalDivider(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 2.dp
                )

                Spacer(modifier = Modifier.height(7.5.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(shadowElevation = 30.dp, border = BorderStroke(2.dp, Color.Black)) {
                        Image(
                            painter = painterResource(id = R.drawable.android_icon),
                            contentScale = ContentScale.Fit,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Song name", fontFamily = fredoka)
                            Text("Rating goes here", fontFamily = fredoka)
                        }
                        Surface(
                            shadowElevation = 30.dp,
                            modifier = Modifier.fillMaxWidth().padding(5.dp)
                        ) {
                            Text("Description", fontFamily = fredoka)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {

                        }, colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5d36eb),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Listen", fontFamily = fredoka)
                    }
                    Button(
                        onClick = {

                        }, colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5d36eb),
                            contentColor = Color.White
                        ), modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("More details", fontFamily = fredoka)
                    }
                }
            }
        }

        Surface(
            color = Color(0xFFbfb1fa),
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 30.dp
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "Friend's Recent Activity",
                    fontFamily = fredoka,
                    fontSize = 26.sp,
                    color = Color.White
                )
            }
        }
    }
}
