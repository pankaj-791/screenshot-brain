package com.screenshotbrain.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.screenshotbrain.ui.brain.BrainRequests
import com.screenshotbrain.ui.brain.BrainScreen
import com.screenshotbrain.ui.collections.CollectionsScreen
import com.screenshotbrain.ui.detail.DetailScreen
import com.screenshotbrain.ui.home.HomeScreen
import com.screenshotbrain.ui.library.LibraryRequests
import com.screenshotbrain.ui.library.LibraryScreen

private const val DETAIL_ROUTE = "detail/{id}"

private enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Home("home", "Home", Icons.Outlined.Home),
    Library("library", "Library", Icons.Outlined.PhotoLibrary),
    Brain("brain", "AI Brain", Icons.Outlined.AutoAwesome),
    Collections("collections", "Collections", Icons.Outlined.Folder),
    Settings("settings", "Settings", Icons.Outlined.Settings)
}

@Composable
fun ScreenshotBrainApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun goToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute != DETAIL_ROUTE) {
                NavigationBar {
                    Destination.values().forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { goToTab(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            Destination.values().forEach { destination ->
                composable(destination.route) {
                    when (destination) {
                        Destination.Home -> HomeScreen(
                            onAsk = { question ->
                                BrainRequests.pending.value = question
                                goToTab(Destination.Brain.route)
                            },
                            onOpenCategory = { category ->
                                LibraryRequests.pendingCategory.value = category
                                goToTab(Destination.Library.route)
                            },
                            onOpenLibrary = { goToTab(Destination.Library.route) },
                            onOpenScreenshot = { id -> navController.navigate("detail/$id") }
                        )
                        Destination.Library -> LibraryScreen(
                            onOpenScreenshot = { id -> navController.navigate("detail/$id") }
                        )
                        Destination.Brain -> BrainScreen(
                            onOpenScreenshot = { id -> navController.navigate("detail/$id") }
                        )
                        Destination.Collections -> CollectionsScreen(
                            onOpenCollection = { /* Opening a collection arrives in Stage 6B */ }
                        )
                        else -> PlaceholderScreen(destination.label)
                    }
                }
            }
            composable(
                route = DETAIL_ROUTE,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                DetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name)
    }
}
