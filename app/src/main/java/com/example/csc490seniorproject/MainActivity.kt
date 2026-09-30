package com.example.csc490seniorproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DiscFull
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.csc490seniorproject.nav.Nav
import com.example.csc490seniorproject.nav.NavItem
import com.example.csc490seniorproject.nav.TopBar
import com.example.csc490seniorproject.ui.theme.CSC490SeniorProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CSC490SeniorProjectTheme {
                var selectedItemIndex by rememberSaveable {
                    mutableStateOf(0) }
                val navHostController = rememberNavController()
                val navBackStackEntry by navHostController.currentBackStackEntryAsState()

                val currentDestination = navBackStackEntry?.destination
                val currentRoute = currentDestination?.route

                val showNavigationBars =
                    currentRoute != null && currentRoute != "LoginScreen" && currentRoute != "RegisterScreen"

                val navItemsList = listOf(
                    NavItem(
                        title = "For You",
                        iconSelected = Icons.Filled.Home,
                        iconUnselected = Icons.Outlined.Home,
                        route = "LandingScreen"
                    ), NavItem(
                        title = "Search",
                        iconSelected = Icons.Filled.Search,
                        iconUnselected = Icons.Outlined.Search,
                        route = "SearchScreen"
                    ), NavItem(
                        title = "Create",
                        iconSelected = Icons.Filled.Add,
                        iconUnselected = Icons.Outlined.Add,
                        route = "EditorScreen"
                    ), NavItem(
                        title = "Profile",
                        iconSelected = Icons.Filled.Person,
                        iconUnselected = Icons.Outlined.PersonOutline,
                        route = "ProfileScreen"
                    )
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),

                    topBar = {
                        if (showNavigationBars) {
                            TopBar(navHostController)
                        }
                    },
                    bottomBar = {
                        if (showNavigationBars) {
                            NavigationBar {
                                navItemsList.forEachIndexed { index, item ->
                                    NavigationBarItem(
                                        selected = currentDestination.hierarchy.any {
                                            it.route == item.route
                                        },
                                        onClick = {
                                            selectedItemIndex = index

                                            navHostController.navigate(item.route) {
                                                launchSingleTop = true
                                                restoreState = true

                                                popUpTo(
                                                    navHostController.graph.findStartDestination().id
                                                ) {
                                                    saveState = true
                                                }
                                            }
                                        },

                                        label = {
                                            Text(text = item.title)
                                        },

                                        icon = {
                                            Icon(
                                                contentDescription = item.title,
                                                imageVector = if (index == selectedItemIndex) {
                                                    item.iconSelected
                                                } else {
                                                    item.iconUnselected
                                                }
                                            )

                                        }
                                    )
                                }
                            }
                        }
                    }
                )
                { innerPadding ->
                    Nav(
                    navController = navHostController, modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

    }
}