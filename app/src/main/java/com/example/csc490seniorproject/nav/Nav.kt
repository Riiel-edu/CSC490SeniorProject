package com.example.csc490seniorproject.nav

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.csc490seniorproject.screens.RegisterScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.csc490seniorproject.viewmodels.ProfileScreenVM

@Composable
fun Nav(navController: NavHostController,
        modifier: Modifier
) {
    val app = LocalContext.current.applicationContext as Application

    val landingScreenVM = viewModel {
        LandingScreenVM(app)
    }

    val searchScreenVM = viewModel {
        SearchScreenVM(app)
    }

    val profileScreenVM = viewModel {
        ProfileScreenVM(app)
    }

    var currentUsername by rememberSaveable {
        mutableStateOf("Username")
    }

    NavHost(
        navController = navController,
        startDestination = "LoginScreen",
        modifier = modifier
    ) {
        composable(route = "LoginScreen") {
            LoginScreen(
                onLoginSuccess = { username -> currentUsername = username

                    navController.navigate("LandingScreen") {
                        popUpTo("LoginScreen") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onRegisterClick = {
                    navController.navigate("RegisterScreen")
                }
            )
        }

        composable(route = "RegisterScreen") {
            RegisterScreen(
                onRegistrationSuccess = { username -> currentUsername = username

                    navController.navigate("LandingScreen") {
                        popUpTo("LoginScreen") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onLoginClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = "LandingScreen") {
            LandingScreen(
                viewModel = landingScreenVM,
                navController = navController,
                username = currentUsername
            )
        }

        composable(route = "SongScreen") {
            SongScreen(landingScreenVM)
        }

        composable(route = "ProfileScreen") {
            ProfileScreen(
                viewModel = profileScreenVM,
                navController = navController,
                username = currentUsername
            )
        }

        composable(route = "SearchScreen") {
            SearchScreen(
                viewModel = searchScreenVM,
                navController = navController
            )
        }
    }
}