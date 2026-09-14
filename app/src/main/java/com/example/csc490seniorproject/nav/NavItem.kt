package com.example.csc490seniorproject.nav

import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem (
    val title: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector,
    val route: String
)