package com.example.smartaccidentdetection.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Tracking : Screen("tracking", "Live Map", Icons.Default.LocationOn)
    object Alerts : Screen("alerts", "Alerts", Icons.Default.Warning)
    object Trips : Screen("trips", "Trip History", Icons.Default.AltRoute)
    object Contacts : Screen("contacts", "Contacts", Icons.Default.ContactPhone)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object DeviceStatus : Screen("device_status", "System Status", Icons.Default.Sensors)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Tracking,
    Screen.Alerts,
    Screen.Contacts,
    Screen.Profile
)
