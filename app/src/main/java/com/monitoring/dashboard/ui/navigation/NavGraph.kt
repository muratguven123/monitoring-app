package com.monitoring.dashboard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.monitoring.dashboard.ui.screen.dashboarddetail.DashboardDetailScreen
import com.monitoring.dashboard.ui.screen.dashboardlist.DashboardListScreen
import com.monitoring.dashboard.ui.screen.settings.SettingsScreen

// ── Route constants ───────────────────────────────────────────────────────────

object Route {
    const val SETTINGS        = "settings"
    const val DASHBOARD_LIST  = "dashboard_list"
    const val DASHBOARD_DETAIL = "dashboard_detail/{uid}/{title}"

    fun dashboardDetail(uid: String, title: String) =
        "dashboard_detail/${uid}/${java.net.URLEncoder.encode(title, "UTF-8")}"
}

// ── NavHost ───────────────────────────────────────────────────────────────────

@Composable
fun MonitoringNavGraph(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
    ) {

        // Settings / onboarding screen
        composable(Route.SETTINGS) {
            SettingsScreen(
                onSetupComplete = {
                    navController.navigate(Route.DASHBOARD_LIST) {
                        popUpTo(Route.SETTINGS) { inclusive = true }
                    }
                },
            )
        }

        // Dashboard list screen
        composable(Route.DASHBOARD_LIST) {
            DashboardListScreen(
                onDashboardClick = { uid, title ->
                    navController.navigate(Route.dashboardDetail(uid, title))
                },
                onSettingsClick = {
                    navController.navigate(Route.SETTINGS)
                },
            )
        }

        // Dashboard detail screen
        composable(
            route = Route.DASHBOARD_DETAIL,
            arguments = listOf(
                navArgument("uid")   { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val uid   = backStackEntry.arguments?.getString("uid")   ?: ""
            val title = backStackEntry.arguments?.getString("title")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
            DashboardDetailScreen(
                uid   = uid,
                title = title,
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
