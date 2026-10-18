package com.example.csc490seniorproject.nav


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.csc490seniorproject.R
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(navController: NavHostController) {
    Surface(
        color = Color(0xFFC2C0FF), contentColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(
                    width = 70.dp,
                    height = 56.dp
                )
            ) {
                Image(
                    painter = painterResource(R.drawable.hippo_image),
                    contentDescription = "HIPPOP",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(44.dp).align(Alignment.TopCenter)
                )

                val letterX = listOf(7, 18, 25, 36, 47, 58)
                val letterY = listOf(33, 36, 37, 37, 36, 33)
                val angles = listOf(16f, 10f, 3f, -3f, -10f, -16f)

                "HIPPOP".forEachIndexed { index, letter ->
                    Text(
                        text = letter.toString(),
                        color = Color(0xFF492395),
                        fontFamily = FontFamily(Font(R.font.fredoka_medium, FontWeight.Medium)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .offset(
                                x = letterX[index].dp,
                                y = letterY[index].dp
                            )
                            .rotate(angles[index])
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    navController.navigate("ProfileScreen") {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(
                            navController.graph.findStartDestination().id
                        ) {
                            saveState = true
                        }
                    }
                }) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = {
                    navController.navigate("SettingsScreen") {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(
                            navController.graph.findStartDestination().id
                        ) {
                            saveState = true
                        }
                    }
                }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}