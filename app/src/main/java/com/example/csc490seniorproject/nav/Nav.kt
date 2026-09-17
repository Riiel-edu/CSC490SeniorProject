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
import com.example.csc490seniorproject.screens.SearchScreen
import com.example.csc490seniorproject.screens.SongScreen
import com.example.csc490seniorproject.viewmodels.LandingScreenVM
import com.example.csc490seniorproject.viewmodels.SearchScreenVM
import com.example.csc490seniorproject.screens.LoginScreen

@Composable
fun Nav(navController: NavHostController, modifier: Modifier) {
    val app = LocalContext.current.applicationContext as Application

    val landingScreenVM = viewModel { LandingScreenVM(app) }
    val searchScreenVM = viewModel { SearchScreenVM(app) }

    NavHost(
        navController = navController,
        startDestination = "LoginScreen",
        modifier = modifier
    ) {
        composable(route = "LoginScreen") {
            LoginScreen(onLoginSuccess = {
                    navController.navigate("LandingScreen") {
                        popUpTo("LoginScreen") {
                            inclusive = true
                        }
                    }
                }
            )
        }

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
            SearchScreen(searchScreenVM)
        }
    }
}