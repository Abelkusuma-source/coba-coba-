package com.at.coba.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Web
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Trade : Screen("trade", "Trade", Icons.Default.ShowChart)
    object History : Screen("history", "History", Icons.Default.History)
    object Web : Screen("web", "Web", Icons.Default.Web)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Debug : Screen("debug", "Debug", Icons.Default.BugReport)
}

val bottomNavItems = listOf(
    Screen.Trade,
    Screen.History,
    Screen.Web,
    Screen.Profile
)
