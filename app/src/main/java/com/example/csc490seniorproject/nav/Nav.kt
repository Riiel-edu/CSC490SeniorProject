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
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.csc490seniorproject.screens.BeatmapEditorScreen
import com.example.csc490seniorproject.screens.EditorScreen
import com.example.csc490seniorproject.screens.GameScreen
import com.example.csc490seniorproject.screens.MySongsScreen
import com.example.csc490seniorproject.screens.SettingsScreen
import com.example.csc490seniorproject.viewmodels.BeatmapEditorVM
import com.example.csc490seniorproject.viewmodels.GameVM

import com.example.csc490seniorproject.viewmodels.ProfileScreenVM
import com.example.csc490seniorproject.viewmodels.SongScreenVM
import com.example.csc490seniorproject.viewmodels.WorkspaceViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("ContextCastToActivity")
@Composable
fun Nav(
    navController: NavHostController,
    modifier: Modifier
) {
    val app = LocalContext.current.applicationContext as Application

    val landingScreenVM = viewModel { LandingScreenVM(app) }
    val searchScreenVM = viewModel { SearchScreenVM(app) }
    val profileScreenVM = viewModel { ProfileScreenVM(app) }
    val songScreenVM = viewModel { SongScreenVM(app) }
    var currentUsername by rememberSaveable {
        mutableStateOf("Username")
    }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null && currentUsername == "Username") {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    document.getString("username")?.let { username ->
                        currentUsername = username
                    }
                }
                .addOnFailureListener { e ->
                    println("NAV: Failed to resolve returning user's username: $e")
                }
        }
    }
    NavHost(
        navController = navController,
        startDestination =
            if (FirebaseAuth.getInstance().currentUser != null) "LandingScreen" else "LoginScreen",
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
            SongScreen(songScreenVM)
        }

        composable("SongScreen/{songId}") { entry ->
            SongScreen(
                viewModel = songScreenVM,
                songId = entry.arguments?.getString("songId").orEmpty()
            )
        }

        composable(route = "ProfileScreen") {

            ProfileScreen(
                viewModel = profileScreenVM,
                navController = navController,
                username = currentUsername,
                profileID = null
            )
        }


        composable(route = "ProfileScreen/{uid}/{username}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid")
            val username = backStackEntry.arguments?.getString("username") ?: ""

            ProfileScreen(
                viewModel = profileScreenVM,
                navController = navController,
                username = username,
                profileID = uid
            )
        }

        composable(route = "SearchScreen") {
            SearchScreen(
                viewModel = searchScreenVM,
                navController = navController,
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
                },
                onLogoutClick = {
                    FirebaseAuth.getInstance().signOut()
                    currentUsername = "Username"

                    navController.navigate("LoginScreen") {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("EditorScreen") {
            val editorViewModel: EditorScreenVM = viewModel()
            LaunchedEffect(Unit) {
                editorViewModel.setOwnerId(FirebaseAuth.getInstance().currentUser?.uid ?: "")
            }
            EditorScreen(
                viewModel = editorViewModel,
                navController = navController,
                modifier = Modifier
            )
        }

        composable("BeatmapEditorScreen") {
            val beatmapViewModel: BeatmapEditorVM = viewModel()
            val workspaceViewModel: WorkspaceViewModel =
                viewModel(LocalContext.current as ComponentActivity)
            val currentSong by workspaceViewModel.currentSong.collectAsState()

            LaunchedEffect(currentSong?.id) {
                beatmapViewModel.startNewBeatmap(
                    songId = currentSong?.id ?: "",
                    ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                )
            }
            BeatmapEditorScreen(
                viewModel = beatmapViewModel,
                navController = navController,
                modifier = Modifier
            )
        }

        composable("GameScreen") {
            val gameViewModel: GameVM = viewModel()
            val workspaceViewModel: WorkspaceViewModel =
                viewModel(LocalContext.current as ComponentActivity)
            val beatmap by workspaceViewModel.currentBeatmap.collectAsState()

            beatmap?.let {
                GameScreen(
                    viewModel = gameViewModel,
                    beatmap = it,
                    navController = navController, // the NavHost's own controller, already in scope here
                    modifier = Modifier
                )
            }
        }
    }

}