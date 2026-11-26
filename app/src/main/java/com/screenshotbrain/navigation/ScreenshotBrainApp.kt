package com.screenshotbrain.navigation

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
import com.screenshotbrain.ui.collections.AddScreenshotsScreen
import com.screenshotbrain.ui.collections.CollectionDetailScreen
import com.screenshotbrain.ui.collections.CollectionsScreen
import com.screenshotbrain.ui.compare.CompareScreen
import com.screenshotbrain.ui.detail.DetailScreen
import com.screenshotbrain.ui.home.HomeScreen
import com.screenshotbrain.ui.library.LibraryRequests
import com.screenshotbrain.ui.library.LibraryScreen
import com.screenshotbrain.ui.notes.NotesScreen
import com.screenshotbrain.ui.reminders.RemindersScreen
import com.screenshotbrain.ui.settings.SettingsScreen

private const val DETAIL_ROUTE = "detail/{id}"
private const val NOTES_ROUTE = "notes/{id}"
private const val COLLECTION_ROUTE = "collection/{id}"
private const val ADD_ROUTE = "collection/{id}/add"
private const val REMINDERS_ROUTE = "reminders"
private const val COMPARE_ROUTE = "compare"

private val fullScreenRoutes = setOf(
    DETAIL_ROUTE, NOTES_ROUTE, COLLECTION_ROUTE, ADD_ROUTE, REMINDERS_ROUTE, COMPARE_ROUTE
)

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

    fun openScreenshot(id: Long) = navController.navigate("detail/$id")

    Scaffold(
        bottomBar = {
            if (currentRoute !in fullScreenRoutes) {
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
                            onOpenReminders = { navController.navigate(REMINDERS_ROUTE) },
                            onOpenScreenshot = { id -> openScreenshot(id) },
                            onOpenCompare = { navController.navigate(COMPARE_ROUTE) }
                        )
                        Destination.Library -> LibraryScreen(
                            onOpenScreenshot = { id -> openScreenshot(id) }
                        )
                        Destination.Brain -> BrainScreen(
                            onOpenScreenshot = { id -> openScreenshot(id) }
                        )
                        Destination.Collections -> CollectionsScreen(
                            onOpenCollection = { id -> navController.navigate("collection/$id") }
                        )
                        Destination.Settings -> SettingsScreen()
                    }
                }
            }
            composable(
                route = DETAIL_ROUTE,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                DetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenNotes = { navController.navigate("notes/$id") }
                )
            }
            composable(
                route = NOTES_ROUTE,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                NotesScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = COLLECTION_ROUTE,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                CollectionDetailScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { id -> navController.navigate("collection/$id/add") },
                    onOpenScreenshot = { id -> openScreenshot(id) }
                )
            }
            composable(
                route = ADD_ROUTE,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                AddScreenshotsScreen(onBack = { navController.popBackStack() })
            }
            composable(REMINDERS_ROUTE) {
                RemindersScreen(
                    onBack = { navController.popBackStack() },
                    onOpenScreenshot = { id -> openScreenshot(id) }
                )
            }
            composable(COMPARE_ROUTE) {
                CompareScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
