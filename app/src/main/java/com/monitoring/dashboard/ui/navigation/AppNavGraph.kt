package com.monitoring.dashboard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.monitoring.dashboard.ui.screens.grafana.GrafanaDashboardDetailScreen
import com.monitoring.dashboard.ui.screens.grafana.GrafanaDashboardsScreen
import com.monitoring.dashboard.ui.screens.home.HomeScreen
import com.monitoring.dashboard.ui.screens.newrelic.NewRelicAppDetailScreen
import com.monitoring.dashboard.ui.screens.newrelic.NewRelicAppsScreen
import com.monitoring.dashboard.ui.screens.settings.SettingsScreen

/**
 * Full application navigation graph.
 *
 * Structure:
 *  ┌─────────────────────────────────────────────────────────┐
 *  │  Bottom nav tabs: Home · Grafana · New Relic · Settings │
 *  └─────────────────────────────────────────────────────────┘
 *       │               │                │
 *  GrafanaDashboardDetail   NewRelicAppDetail
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        modifier         = modifier,
    ) {

        // ── Home ───────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGrafana       = { navController.navigate(Screen.Grafana.route) },
                onNavigateToNewRelic      = { navController.navigate(Screen.NewRelic.route) },
                onNavigateToGrafanaDashboard = { uid ->
                    navController.navigate(Screen.GrafanaDashboardDetail.createRoute(uid))
                },
                onNavigateToNewRelicApp = { appId ->
                    navController.navigate(Screen.NewRelicAppDetail.createRoute(appId))
                },
            )
        }

        // ── Grafana Dashboards List ────────────────────────────────────────
        composable(Screen.Grafana.route) {
            GrafanaDashboardsScreen(
                onDashboardClick = { uid ->
                    navController.navigate(Screen.GrafanaDashboardDetail.createRoute(uid))
                },
            )
        }

        // ── Grafana Dashboard Detail ───────────────────────────────────────
        composable(
            route = Screen.GrafanaDashboardDetail.route,
            arguments = listOf(
                navArgument("uid") { type = NavType.StringType },
            ),
        ) {
            GrafanaDashboardDetailScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── New Relic Apps List ────────────────────────────────────────────
        composable(Screen.NewRelic.route) {
            NewRelicAppsScreen(
                onAppClick = { appId ->
                    navController.navigate(Screen.NewRelicAppDetail.createRoute(appId))
                },
            )
        }

        // ── New Relic App Detail ───────────────────────────────────────────
        composable(
            route = Screen.NewRelicAppDetail.route,
            arguments = listOf(
                navArgument("appId") { type = NavType.LongType },
            ),
        ) {
            NewRelicAppDetailScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── Settings ───────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
