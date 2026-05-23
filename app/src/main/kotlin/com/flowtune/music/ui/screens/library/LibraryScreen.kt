package com.flowtune.music.ui.screens.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun LibraryScreen(navController: NavController) {
    LibraryMixScreen(
        navController = navController,
        filterContent = {},
    )
}