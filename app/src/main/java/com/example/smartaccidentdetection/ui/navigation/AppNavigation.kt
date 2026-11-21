package com.example.smartaccidentdetection.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smartaccidentdetection.data.repository.AppRepository
import com.example.smartaccidentdetection.ui.screens.alerts.AlertsScreen
import com.example.smartaccidentdetection.ui.screens.contacts.EmergencyContactsScreen
import com.example.smartaccidentdetection.ui.screens.dashboard.DashboardScreen
import com.example.smartaccidentdetection.ui.screens.profile.ProfileScreen
import com.example.smartaccidentdetection.ui.screens.settings.SettingsScreen
import com.example.smartaccidentdetection.ui.screens.status.DeviceStatusScreen
import com.example.smartaccidentdetection.ui.screens.tracking.LiveTrackingScreen
import com.example.smartaccidentdetection.ui.screens.trips.TripHistoryScreen
import com.example.smartaccidentdetection.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    repository: AppRepository = AppRepository.getInstance()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val currentScreenTitle = when (currentRoute) {
        Screen.Dashboard.route -> "Smart Accident Detection"
        Screen.Tracking.route -> "Live Vehicle Tracking"
        Screen.Alerts.route -> "Accident Incident Alerts"
        Screen.Trips.route -> "Trip & Journey History"
        Screen.Contacts.route -> "Emergency Contacts"
        Screen.Profile.route -> "Rider Profile"
        Screen.DeviceStatus.route -> "System & Sensor Diagnostics"
        Screen.Settings.route -> "App & Hardware Settings"
        else -> "Smart Accident Detection"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentScreenTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    // Trips shortcut button
                    IconButton(onClick = {
                        if (currentRoute != Screen.Trips.route) {
                            navController.navigate(Screen.Trips.route) {
                                launchSingleTop = true
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.AltRoute,
                            contentDescription = "Trips",
                            tint = if (currentRoute == Screen.Trips.route) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Diagnostics shortcut button
                    IconButton(onClick = {
                        if (currentRoute != Screen.DeviceStatus.route) {
                            navController.navigate(Screen.DeviceStatus.route) {
                                launchSingleTop = true
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Diagnostics",
                            tint = if (currentRoute == Screen.DeviceStatus.route) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Settings shortcut button
                    IconButton(onClick = {
                        if (currentRoute != Screen.Settings.route) {
                            navController.navigate(Screen.Settings.route) {
                                launchSingleTop = true
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (currentRoute == Screen.Settings.route) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                if (screen.route == Screen.Tracking.route) {
                                    repository.clearFocusedIncident()
                                }
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            indicatorColor = PrimaryBlue.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    repository = repository,
                    onNavigateToTracking = {
                        repository.clearFocusedIncident()
                        navController.navigate(Screen.Tracking.route)
                    },
                    onNavigateToAlerts = { navController.navigate(Screen.Alerts.route) },
                    onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                    onNavigateToDiagnostics = { navController.navigate(Screen.DeviceStatus.route) }
                )
            }

            composable(Screen.Tracking.route) {
                LiveTrackingScreen(repository = repository)
            }

            composable(Screen.Alerts.route) {
                AlertsScreen(
                    repository = repository,
                    onNavigateToTracking = {
                        navController.navigate(Screen.Tracking.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Trips.route) {
                TripHistoryScreen(
                    repository = repository,
                    onNavigateToTracking = { navController.navigate(Screen.Tracking.route) }
                )
            }

            composable(Screen.Contacts.route) {
                EmergencyContactsScreen(repository = repository)
            }

            composable(Screen.Profile.route) {
                ProfileScreen(repository = repository)
            }

            composable(Screen.DeviceStatus.route) {
                DeviceStatusScreen(repository = repository)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(repository = repository)
            }
        }
    }
}
