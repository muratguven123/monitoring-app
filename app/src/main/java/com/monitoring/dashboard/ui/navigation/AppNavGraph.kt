package com.monitoring.dashboard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.monitoring.dashboard.ui.screens.alerts.AlertsScreen
import com.monitoring.dashboard.ui.screens.datasources.DatasourcesScreen
import com.monitoring.dashboard.ui.screens.github.GithubStatusScreen
import com.monitoring.dashboard.ui.screens.grafana.GrafanaDashboardDetailScreen
import com.monitoring.dashboard.ui.screens.grafana.GrafanaDashboardsScreen
import com.monitoring.dashboard.ui.screens.grafana.GrafanaPanelDetailScreen
import com.monitoring.dashboard.ui.screens.home.HomeScreen
import com.monitoring.dashboard.ui.screens.lock.AppLockScreen
import com.monitoring.dashboard.ui.screens.newrelic.NewRelicAppDetailScreen
import com.monitoring.dashboard.ui.screens.newrelic.NewRelicAppsScreen
import com.monitoring.dashboard.ui.screens.newrelic.NewRelicMetricDetailScreen
import com.monitoring.dashboard.ui.screens.nrql.NrqlScreen
import com.monitoring.dashboard.ui.screens.onboarding.OnboardingScreen
import com.monitoring.dashboard.ui.screens.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    onUnlocked: () -> Unit = {},
    onOnboardingFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    onOnboardingFinished()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Lock.route) {
            AppLockScreen(
                onUnlocked = {
                    onUnlocked()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Lock.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGrafana = { navController.navigate(Screen.Grafana.route) },
                onNavigateToNewRelic = { navController.navigate(Screen.NewRelic.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAlerts = { navController.navigate(Screen.Alerts.route) },
                onNavigateToGrafanaDashboard = { uid ->
                    navController.navigate(Screen.GrafanaDashboardDetail.createRoute(uid))
                },
                onNavigateToNewRelicApp = { appId ->
                    navController.navigate(Screen.NewRelicAppDetail.createRoute(appId))
                },
                onNavigateToGithub = { navController.navigate(Screen.GithubStatus.route) },
                onNavigateToNrql = { navController.navigate(Screen.Nrql.route) },
            )
        }

        composable(
            route = Screen.Alerts.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "monitoring://alerts" },
            ),
        ) {
            AlertsScreen()
        }

        composable(Screen.Grafana.route) {
            GrafanaDashboardsScreen(
                onDashboardClick = { uid ->
                    navController.navigate(Screen.GrafanaDashboardDetail.createRoute(uid))
                },
                onDatasourcesClick = { navController.navigate(Screen.Datasources.route) },
            )
        }

        composable(
            route = Screen.GrafanaDashboardDetail.route,
            arguments = listOf(navArgument("uid") { type = NavType.StringType }),
        ) {
            GrafanaDashboardDetailScreen(
                onBackClick = { navController.popBackStack() },
                onPanelClick = { uid, panelId, slug, panelTitle ->
                    navController.navigate(
                        Screen.GrafanaPanelDetail.createRoute(uid, panelId, slug, panelTitle),
                    )
                },
            )
        }

        composable(
            route = Screen.GrafanaPanelDetail.route,
            arguments = listOf(
                navArgument("uid") { type = NavType.StringType },
                navArgument("panelId") { type = NavType.LongType },
                navArgument("slug") { type = NavType.StringType; defaultValue = "" },
                navArgument("panelTitle") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            GrafanaPanelDetailScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Datasources.route) {
            DatasourcesScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.NewRelic.route) {
            NewRelicAppsScreen(
                onAppClick = { appId ->
                    navController.navigate(Screen.NewRelicAppDetail.createRoute(appId))
                },
                onNrqlClick = { navController.navigate(Screen.Nrql.route) },
            )
        }

        composable(
            route = Screen.NewRelicAppDetail.route,
            arguments = listOf(navArgument("appId") { type = NavType.LongType }),
        ) {
            NewRelicAppDetailScreen(
                onBackClick = { navController.popBackStack() },
                onMetricClick = { appId, metricName, valueKey, displayName, unit ->
                    navController.navigate(
                        Screen.NewRelicMetricDetail.createRoute(
                            appId, metricName, valueKey, displayName, unit,
                        ),
                    )
                },
            )
        }

        composable(
            route = Screen.NewRelicMetricDetail.route,
            arguments = listOf(
                navArgument("appId") { type = NavType.LongType },
                navArgument("metricName") { type = NavType.StringType },
                navArgument("valueKey") { type = NavType.StringType; defaultValue = "" },
                navArgument("displayName") { type = NavType.StringType; defaultValue = "" },
                navArgument("unit") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            NewRelicMetricDetailScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Nrql.route) {
            NrqlScreen(
                onBackClick = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.GithubStatus.route) {
            GithubStatusScreen(
                onBackClick = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
