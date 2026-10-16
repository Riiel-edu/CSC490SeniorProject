package com.example.csc490seniorproject.nav

import android.annotation.SuppressLint
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.csc490seniorproject.viewmodels.EditorScreenVM
import com.example.csc490seniorproject.screens.RegisterScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.csc490seniorproject.screens.BeatmapEditorScreen
import com.example.csc490seniorproject.screens.EditorScreen
import com.example.csc490seniorproject.screens.GameScreen
import com.example.csc490seniorproject.screens.MySongsScreen
import com.example.csc490seniorproject.screens.SettingsScreen
import com.example.csc490seniorproject.viewmodels.BeatmapEditorVM
import com.example.csc490seniorproject.viewmodels.GameVM

import com.example.csc490seniorproject.viewmodels.ProfileScreenVM
import com.example.csc490seniorproject.viewmodels.WorkspaceViewModel
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("ContextCastToActivity")
@Composable
fun Nav(navController: NavHostController,
        modifier: Modifier
) {
    val app = LocalContext.current.applicationContext as Application

    val landingScreenVM = viewModel { LandingScreenVM(app) }
    val searchScreenVM = viewModel { SearchScreenVM(app) }
    val profileScreenVM = viewModel { ProfileScreenVM(app) }
    val beatmapViewModel = viewModel { BeatmapEditorVM(app) }
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
                onLoginSuccess = { username ->
                    currentUsername = username

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
                onRegistrationSuccess = { username ->
                    currentUsername = username

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

        composable(route = "MySongsScreen") {
            MySongsScreen(
                viewModel = searchScreenVM,
                navController = navController
            )
        }

        composable(route = "SettingsScreen") {
            SettingsScreen(
                viewModel = landingScreenVM,
                onProfileClick = {
                    navController.navigate("ProfileScreen")
                }
            )
        }

        composable("EditorScreen") {
            val editorViewModel: EditorScreenVM = viewModel()
            LaunchedEffect(Unit) {
                editorViewModel.setOwnerId(FirebaseAuth.getInstance().currentUser?.uid ?: "")
            }
            EditorScreen(viewModel = editorViewModel, navController = navController, modifier = Modifier)
        }

        composable("BeatmapEditorScreen") {
            val beatmapViewModel: BeatmapEditorVM = viewModel()
            val workspaceViewModel: WorkspaceViewModel = viewModel(LocalContext.current as ComponentActivity)
            val currentSong by workspaceViewModel.currentSong.collectAsState()

            LaunchedEffect(currentSong?.id) {
                beatmapViewModel.startNewBeatmap(
                    songId = currentSong?.id ?: "",
                    ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                )
            }
            BeatmapEditorScreen(viewModel = beatmapViewModel, navController = navController, modifier = Modifier)
        }

        composable("GameScreen") {
            val gameViewModel: GameVM = viewModel()
            val workspaceViewModel: WorkspaceViewModel = viewModel(LocalContext.current as ComponentActivity)
            val beatmap by workspaceViewModel.currentBeatmap.collectAsState()

            beatmap?.let {
                GameScreen(viewModel = gameViewModel, beatmap = it, modifier = Modifier)
            }
        }

        composable(route = "BeatmapEditorScreen") {
            LaunchedEffect(Unit) {
                beatmapViewModel.startNewBeatmap(
                    songId = "NULL",
                    ownerId = "NULL"
                )
            }
            BeatmapEditorScreen(viewModel = beatmapViewModel, navController, modifier = Modifier)
        }
    }
}