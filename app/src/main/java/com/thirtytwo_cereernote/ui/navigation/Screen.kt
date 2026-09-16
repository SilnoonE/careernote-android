package com.thirtytwo_cereernote.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.thirtytwo_cereernote.R

sealed class Screen(val route: String, val titleResId: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object Applications : Screen("applications", R.string.nav_applications, Icons.AutoMirrored.Filled.List)
    object Career : Screen("career", R.string.nav_career, Icons.Default.Person)
    object Report : Screen("report", R.string.nav_report, Icons.Default.Notifications)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Applications,
    Screen.Career,
    Screen.Report,
    Screen.Settings
)
