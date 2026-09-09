package com.example.csc490seniorproject.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.csc490seniorproject.viewmodels.LandingScreenVM

@Composable
fun LandingScreen(viewModel: LandingScreenVM, modifier: Modifier) {

    Column(modifier) {
        Text("Hello World!")
    }
}