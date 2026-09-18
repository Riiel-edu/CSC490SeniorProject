package com.example.csc490seniorproject.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.csc490seniorproject.R
import com.example.csc490seniorproject.viewmodels.LandingScreenVM
import com.example.csc490seniorproject.viewmodels.SearchScreenVM

@Composable
fun SearchScreen(viewModel: SearchScreenVM, navController: NavController) {

    val songsList = viewModel.songsList

    val fredoka = FontFamily(Font(R.font.fredoka_medium, FontWeight.Normal))

    var text by remember { mutableStateOf("") }

    Column(Modifier
        .fillMaxWidth()
        .padding(5.dp)
    ) {

        Spacer(modifier = Modifier.height(5.dp))

        TextField(value = text, onValueChange = { text = it }, label = { Text("Search") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(songsList) { currentItem ->
                //Song block start
                Surface(
                    color = Color(0xFFbfb1fa),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 30.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shadowElevation = 8.dp,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(2.dp, Color.Black)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.android_icon),
                                    contentScale = ContentScale.Crop,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        currentItem.name,
                                        fontFamily = fredoka,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if(currentItem.rating > 4.5) {
                                        Text(
                                            buildAnnotatedString {
                                                withStyle(style = SpanStyle(color = Color(0xFF5d36eb))) {
                                                    append("★★★★★")
                                                }
                                            },
                                            fontFamily = fredoka,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else if(currentItem.rating > 3.5) {
                                        Text(
                                            buildAnnotatedString {
                                                withStyle(style = SpanStyle(color = Color(0xFF5d36eb))) {
                                                    append("★★★★")
                                                }
                                                withStyle(style = SpanStyle(color = Color.White)) {
                                                    append("★")
                                                }
                                            },
                                            fontFamily = fredoka,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else if(currentItem.rating > 2.5) {
                                        Text(
                                            buildAnnotatedString {
                                                withStyle(style = SpanStyle(color = Color(0xFF5d36eb))) {
                                                    append("★★★")
                                                }
                                                withStyle(style = SpanStyle(color = Color.White)) {
                                                    append("★★")
                                                }
                                            },
                                            fontFamily = fredoka,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else if(currentItem.rating > 1.5) {
                                        Text(
                                            buildAnnotatedString {
                                                withStyle(style = SpanStyle(color = Color(0xFF5d36eb))) {
                                                    append("★★")
                                                }
                                                withStyle(style = SpanStyle(color = Color.White)) {
                                                    append("★★★")
                                                }
                                            },
                                            fontFamily = fredoka,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Text(
                                            buildAnnotatedString {
                                                withStyle(style = SpanStyle(color = Color(0xFF5d36eb))) {
                                                    append("★")
                                                }
                                                withStyle(style = SpanStyle(color = Color.White)) {
                                                    append("★★★★")
                                                }
                                            },
                                            fontFamily = fredoka,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    shadowElevation = 4.dp,
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Description",
                                        fontFamily = fredoka,
                                        fontSize = 13.sp,
                                        color = Color(0xFF444444),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5d36eb),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "Listen \uD83D\uDD0A",
                                    fontFamily = fredoka,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = {
                                    navController.navigate("SongScreen")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5d36eb),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1.6f)
                            ) {
                                Text(
                                    "More details ☰",
                                    fontFamily = fredoka,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } // Song block end
            }
        }
    }
}
