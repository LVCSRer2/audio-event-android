package com.audiolifelog.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.audiolifelog.app.ui.export.ExportScreen
import com.audiolifelog.app.ui.home.HomeScreen
import com.audiolifelog.app.ui.settings.SettingsScreen
import com.audiolifelog.app.ui.statistics.StatisticsScreen
import com.audiolifelog.app.ui.timeline.TimelineScreen

sealed class Screen(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    data object Home : Screen("home", Icons.Default.Home, "Home")
    data object Timeline : Screen("timeline", Icons.Default.DateRange, "Timeline")
    data object Statistics : Screen("statistics", Icons.Default.PieChart, "Statistics")
    data object Settings : Screen("settings", Icons.Default.Settings, "Settings")
    data object Export : Screen("export", Icons.Default.Share, "Export")
}

private val bottomNavItems = listOf(
    Screen.Home,
    Screen.Timeline,
    Screen.Statistics,
    Screen.Settings
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar || currentDestination?.route == null) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == screen.route
                            } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Timeline.route) {
                TimelineScreen()
            }
            composable(Screen.Statistics.route) {
                StatisticsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToExport = {
                        navController.navigate(Screen.Export.route)
                    }
                )
            }
            composable(Screen.Export.route) {
                ExportScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
