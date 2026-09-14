package com.example.csc490seniorproject.nav

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.csc490seniorproject.screens.LandingScreen
import com.example.csc490seniorproject.screens.ProfileScreen
import com.example.csc490seniorproject.screens.SongScreen
import com.example.csc490seniorproject.viewmodels.LandingScreenVM

@Composable
fun Nav(navController: NavHostController, modifier: Modifier) {
    val app = LocalContext.current.applicationContext as Application

    val landingScreenVM = viewModel { LandingScreenVM(app) }

    NavHost(navController=navController, startDestination = "LandingScreen", modifier) {
        composable(route = "LandingScreen") {
            LandingScreen(landingScreenVM)
        }
        composable(route = "SongScreen") {
            SongScreen(landingScreenVM)
        }
        composable(route = "ProfileScreen") {
            ProfileScreen(landingScreenVM)
        }
        composable(route = "SearchScreen") {
            ProfileScreen(landingScreenVM)
        }
    }
}