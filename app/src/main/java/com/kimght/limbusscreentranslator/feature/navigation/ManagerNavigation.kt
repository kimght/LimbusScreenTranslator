package com.kimght.limbusscreentranslator.feature.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Map
import com.composables.icons.lucide.Settings
import com.kimght.limbusscreentranslator.R
import com.kimght.limbusscreentranslator.feature.detail.DetailScreen
import com.kimght.limbusscreentranslator.feature.home.HomeScreen
import com.kimght.limbusscreentranslator.feature.library.LibraryScreen
import com.kimght.limbusscreentranslator.feature.settings.SettingsScreen
import com.kimght.limbusscreentranslator.ui.theme.BgBackground
import com.kimght.limbusscreentranslator.ui.theme.Limbus300
import com.kimght.limbusscreentranslator.ui.theme.Limbus500
import kotlinx.coroutines.launch

internal object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{sourceName}/{id}"

    fun detail(sourceName: String, id: String): String =
        "detail/${android.net.Uri.encode(sourceName)}/${android.net.Uri.encode(id)}"
}

private data class TopDestination(val route: String, @StringRes val label: Int, val icon: ImageVector)

@Composable
fun ManagerApp(
    onOpenOverlay: () -> Unit,
    onCloseOverlay: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val topDestinations = listOf(
        TopDestination(Routes.HOME, R.string.nav_home, Lucide.House),
        TopDestination(Routes.LIBRARY, R.string.nav_library, Lucide.Map),
        TopDestination(Routes.SETTINGS, R.string.nav_settings, Lucide.Settings),
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = topDestinations.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    Scaffold(
        modifier = modifier,
        containerColor = BgBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    destinations = topDestinations,
                    currentDestination = currentDestination,
                    onNavigate = { route -> navController.navigateTop(route) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenDetail = { source, id ->
                        navController.navigate(
                            Routes.detail(
                                source,
                                id
                            )
                        )
                    },
                    onBrowseLibrary = { navController.navigateTop(Routes.LIBRARY) },
                    onOpenOverlay = onOpenOverlay,
                    onCloseOverlay = onCloseOverlay,
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onOpenDetail = { source, id ->
                        navController.navigate(
                            Routes.detail(
                                source,
                                id
                            )
                        )
                    },
                    onOpenSettings = { navController.navigateTop(Routes.SETTINGS) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onMessage = showMessage)
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(
                    androidx.navigation.navArgument("sourceName") {
                        type = androidx.navigation.NavType.StringType
                    },
                    androidx.navigation.navArgument("id") {
                        type = androidx.navigation.NavType.StringType
                    },
                ),
            ) {
                DetailScreen(
                    onBack = { navController.popBackStack() },
                    onUninstalled = { navController.popBackStack() },
                    onOpenOverlay = onOpenOverlay,
                    onCloseOverlay = onCloseOverlay,
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    destinations: List<TopDestination>,
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(containerColor = BgBackground) {
        destinations.forEach { destination ->
            val selected =
                currentDestination?.hierarchy?.any { it.route == destination.route } == true
            val label = stringResource(destination.label)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Box(contentAlignment = Alignment.Center) {
                        if (selected) {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Limbus300.copy(alpha = 0.25f),
                                                Color.Transparent
                                            ),
                                        ),
                                    ),
                            )
                        }
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = label,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) Limbus300 else Limbus500,
                        )
                    }
                },
                label = {
                    Text(
                        text = label.uppercase(),
                        fontSize = 9.sp,
                        letterSpacing = 1.0.sp,
                        fontWeight = FontWeight.Medium,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Limbus300,
                    selectedTextColor = Limbus300,
                    unselectedIconColor = Limbus500,
                    unselectedTextColor = Limbus500,
                    indicatorColor = BgBackground,
                ),
            )
        }
    }
}

private fun NavHostController.navigateTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
