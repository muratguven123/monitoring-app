package com.monitoring.dashboard.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.monitoring.dashboard.R

sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int? = null,
    val icon: ImageVector? = null,
) {
    data object Onboarding : Screen("onboarding", R.string.action_get_started)

    data object Home : Screen("home", R.string.nav_dashboard, Icons.Default.Dashboard)
    data object Alerts : Screen("alerts", R.string.screen_alerts_title, Icons.Default.Notifications)
    data object Grafana : Screen("grafana", R.string.nav_grafana, Icons.Default.MonitorHeart)
    data object NewRelic : Screen("newrelic", R.string.nav_newrelic, Icons.Default.Insights)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)

    data object GrafanaDashboardDetail : Screen("grafana/dashboard/{uid}", R.string.screen_grafana_detail_title) {
        fun createRoute(uid: String) = "grafana/dashboard/$uid"
    }

    data object GrafanaPanelDetail : Screen(
        route = "grafana/dashboard/{uid}/panel/{panelId}?slug={slug}&panelTitle={panelTitle}",
        titleRes = R.string.screen_grafana_detail_title,
    ) {
        fun createRoute(uid: String, panelId: Long, slug: String, panelTitle: String): String {
            val encodedSlug = java.net.URLEncoder.encode(slug, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(panelTitle, "UTF-8")
            return "grafana/dashboard/$uid/panel/$panelId?slug=$encodedSlug&panelTitle=$encodedTitle"
        }
    }

    data object NewRelicAppDetail : Screen("newrelic/app/{appId}", R.string.screen_newrelic_app_detail_title) {
        fun createRoute(appId: Long) = "newrelic/app/$appId"
    }

    data object NewRelicMetricDetail : Screen(
        route = "newrelic/app/{appId}/metric/{metricName}?valueKey={valueKey}&displayName={displayName}&unit={unit}",
        titleRes = R.string.screen_newrelic_app_detail_title,
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

    data object Datasources : Screen("grafana/datasources", R.string.screen_datasources_title)
    data object Nrql : Screen("newrelic/nrql", R.string.screen_nrql_title)
    data object GithubStatus : Screen("github/status", R.string.screen_github_title)
    data object Lock : Screen("lock", R.string.lock_title)

    companion object {
        val bottomNavItems = listOf(Home, Alerts, Grafana, NewRelic, Settings)
        const val EXTRA_DESTINATION = "extra_destination"
        const val DEST_ALERTS = "alerts"
    }
}
