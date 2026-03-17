package com.monitoring.dashboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.monitoring.dashboard.ui.navigation.AppNavGraph
import com.monitoring.dashboard.ui.navigation.Screen

/**
 * Root composable that hosts the [Scaffold] with a bottom navigation bar
 * and the full [AppNavGraph].
 *
 * Bottom nav items: Home · Grafana · New Relic · Settings
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Screens where the bottom bar should be visible
    val bottomBarRoutes = Screen.bottomNavItems.map { it.route }.toSet()
    val showBottomBar = currentDestination?.route in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentDestination
                            ?.hierarchy
                            ?.any { it.route == screen.route } == true

                        NavigationBarItem(
                            icon = {
                                screen.icon?.let { Icon(it, contentDescription = screen.title) }
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    // Pop up to start destination to avoid large back stack
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        AppNavGraph(
            navController     = navController,
            startDestination  = Screen.Home.route,
            modifier          = Modifier.padding(innerPadding),
        )
    }
}
