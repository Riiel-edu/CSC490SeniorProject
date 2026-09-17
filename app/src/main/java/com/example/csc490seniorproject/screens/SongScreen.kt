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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csc490seniorproject.R
import com.example.csc490seniorproject.viewmodels.LandingScreenVM

@Composable
fun SongScreen(viewModel: LandingScreenVM) {
    val fredoka = FontFamily(Font(R.font.fredoka_medium, FontWeight.Normal))

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {

        Spacer(modifier = Modifier.height(2.5.dp))

        HorizontalDivider(
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            thickness = 2.dp
        )

        Spacer(modifier = Modifier.height(7.5.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                shadowElevation = 30.dp,
                border = BorderStroke(2.dp, Color(0xFF5d36eb))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.android_icon),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp)
                )
            }

            Column(
                modifier = Modifier
                    .height(120.dp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Username", fontFamily = fredoka, fontSize = 24.sp)

                Text("★★★★★", fontFamily = fredoka, fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text("Description", fontFamily = fredoka, fontSize = 24.sp)

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 4.dp,
            border = BorderStroke(2.dp, Color(0xFF5d36eb)),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "This is the description. Users are free to type a brief description for users to see what the song is about.",
                fontFamily = fredoka,
                fontSize = 13.sp,
                color = Color(0xFF444444),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp,
                color = Color(0xFF5d36eb),
                modifier = Modifier.weight(1f),
                border = BorderStroke(2.dp, Color(0xFF5d36eb)),
                onClick = { }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.comment),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Comments",
                        fontFamily = fredoka,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp,
                color = Color(0xFF5d36eb),
                modifier = Modifier.weight(1f),
                border = BorderStroke(2.dp, Color(0xFF5d36eb)),
                onClick = { }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.leaderboard),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Leaderboard",
                        fontFamily = fredoka,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
