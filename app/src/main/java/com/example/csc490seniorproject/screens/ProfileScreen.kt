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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csc490seniorproject.R
import com.example.csc490seniorproject.viewmodels.LandingScreenVM

@Composable
fun ProfileScreen(viewModel: LandingScreenVM) {
    val fredoka = FontFamily(Font(R.font.fredoka_medium, FontWeight.Normal))

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(2.5.dp))

        HorizontalDivider(
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            thickness = 2.dp
        )

        Spacer(modifier = Modifier.height(7.5.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(25.dp)
        ) {
            Surface(
                shape = CircleShape,
                shadowElevation = 30.dp,
                border = BorderStroke(2.dp, Color.Black)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.android_icon),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            }

            Column(
                modifier = Modifier
                    .height(120.dp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Username", fontFamily = fredoka, fontSize = 24.sp)

                Button(
                    onClick = {

                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5d36eb),
                        contentColor = Color.White
                    )
                ) {
                    Text("✎ Edit Profile", fontFamily = fredoka)
                }
            }
        }
    }
}
