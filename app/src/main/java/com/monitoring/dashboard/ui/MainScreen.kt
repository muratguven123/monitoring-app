package com.monitoring.dashboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.ui.navigation.AppNavGraph
import com.monitoring.dashboard.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val securePreferencesManager: SecurePreferencesManager,
) : ViewModel() {
    private val _needsOnboarding = MutableStateFlow(!securePreferencesManager.isOnboardingComplete())
    val needsOnboarding: StateFlow<Boolean> = _needsOnboarding.asStateFlow()

    private val _needsLock = MutableStateFlow(securePreferencesManager.isAppLockEnabled())
    val needsLock: StateFlow<Boolean> = _needsLock.asStateFlow()

    fun refreshGates() {
        _needsOnboarding.value = !securePreferencesManager.isOnboardingComplete()
        _needsLock.value = securePreferencesManager.isAppLockEnabled()
    }
}

@Composable
fun MainScreen(
    deepLinkDestination: String? = null,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val needsOnboarding by viewModel.needsOnboarding.collectAsStateWithLifecycle()
    val needsLock by viewModel.needsLock.collectAsStateWithLifecycle()

    val startDestination = when {
        needsOnboarding -> Screen.Onboarding.route
        needsLock -> Screen.Lock.route
        deepLinkDestination == Screen.DEST_ALERTS -> Screen.Alerts.route
        else -> Screen.Home.route
    }

    var handledDeepLink by remember { mutableStateOf(false) }
    LaunchedEffect(deepLinkDestination, needsOnboarding, needsLock) {
        if (!needsOnboarding && !needsLock && deepLinkDestination == Screen.DEST_ALERTS && !handledDeepLink) {
            handledDeepLink = true
            navController.navigate(Screen.Alerts.route) {
                launchSingleTop = true
            }
        }
    }

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
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        AppNavGraph(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
