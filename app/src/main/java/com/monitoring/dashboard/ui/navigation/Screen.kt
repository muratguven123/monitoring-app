package com.monitoring.dashboard.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
) {
    data object Onboarding : Screen("onboarding", "Setup")

    data object Home : Screen("home", "Dashboard", Icons.Default.Dashboard)
    data object Alerts : Screen("alerts", "Alerts", Icons.Default.Notifications)
    data object Grafana : Screen("grafana", "Grafana", Icons.Default.MonitorHeart)
    data object NewRelic : Screen("newrelic", "New Relic", Icons.Default.Insights)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    data object GrafanaDashboardDetail : Screen("grafana/dashboard/{uid}", "Dashboard Detail") {
        fun createRoute(uid: String) = "grafana/dashboard/$uid"
    }

    data object GrafanaPanelDetail : Screen(
        route = "grafana/dashboard/{uid}/panel/{panelId}?slug={slug}&panelTitle={panelTitle}",
        title = "Panel Detail",
    ) {
        fun createRoute(uid: String, panelId: Long, slug: String, panelTitle: String): String {
            val encodedSlug = java.net.URLEncoder.encode(slug, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(panelTitle, "UTF-8")
            return "grafana/dashboard/$uid/panel/$panelId?slug=$encodedSlug&panelTitle=$encodedTitle"
        }
    }

    data object NewRelicAppDetail : Screen("newrelic/app/{appId}", "App Detail") {
        fun createRoute(appId: Long) = "newrelic/app/$appId"
    }

    data object NewRelicMetricDetail : Screen(
        route = "newrelic/app/{appId}/metric/{metricName}?valueKey={valueKey}&displayName={displayName}&unit={unit}",
        title = "Metric Detail",
    ) {
        fun createRoute(
            appId: Long,
            metricName: String,
            valueKey: String,
            displayName: String,
            unit: String,
        ): String {
            val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8") }
            return "newrelic/app/$appId/metric/${enc(metricName)}" +
                "?valueKey=${enc(valueKey)}&displayName=${enc(displayName)}&unit=${enc(unit)}"
        }
    }

    data object Datasources : Screen("grafana/datasources", "Datasources")
    data object Nrql : Screen("newrelic/nrql", "NRQL")
    data object GithubStatus : Screen("github/status", "GitHub")
    data object Lock : Screen("lock", "Unlock")

    companion object {
        val bottomNavItems = listOf(Home, Alerts, Grafana, NewRelic, Settings)
        const val EXTRA_DESTINATION = "extra_destination"
        const val DEST_ALERTS = "alerts"
    }
}
