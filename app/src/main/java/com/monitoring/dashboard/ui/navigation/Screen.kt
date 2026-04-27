package com.monitoring.dashboard.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
) {
    // Bottom navigation destinations
    data object Home : Screen("home", "Dashboard", Icons.Default.Dashboard)
    data object Grafana : Screen("grafana", "Grafana", Icons.Default.MonitorHeart)
    data object NewRelic : Screen("newrelic", "New Relic", Icons.Default.Insights)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    // Detail screens
    data object GrafanaDashboardDetail : Screen("grafana/dashboard/{uid}", "Dashboard Detail") {
        fun createRoute(uid: String) = "grafana/dashboard/$uid"
    }

    data object GrafanaPanelDetail : Screen(
        route = "grafana/dashboard/{uid}/panel/{panelId}?slug={slug}&panelTitle={panelTitle}",
        title = "Panel Detail",
    ) {
        fun createRoute(uid: String, panelId: Long, slug: String, panelTitle: String): String {
            val encodedSlug  = java.net.URLEncoder.encode(slug, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(panelTitle, "UTF-8")
            return "grafana/dashboard/$uid/panel/$panelId?slug=$encodedSlug&panelTitle=$encodedTitle"
        }
    }

    data object NewRelicAppDetail : Screen("newrelic/app/{appId}", "App Detail") {
        fun createRoute(appId: Long) = "newrelic/app/$appId"
    }

    companion object {
        val bottomNavItems = listOf(Home, Grafana, NewRelic, Settings)
    }
}
