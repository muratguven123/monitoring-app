package com.monitoring.dashboard.ui.screens.demo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monitoring.dashboard.ui.theme.GrafanaOrange
import com.monitoring.dashboard.ui.theme.NewRelicGreen
import com.monitoring.dashboard.ui.theme.StatusCritical
import com.monitoring.dashboard.ui.theme.StatusGray
import com.monitoring.dashboard.ui.theme.StatusHealthy
import com.monitoring.dashboard.ui.theme.StatusWarning
import com.monitoring.dashboard.ui.theme.Surface as AppSurface
import com.monitoring.dashboard.ui.theme.SurfaceBright
import com.monitoring.dashboard.ui.theme.SurfaceVariant

// ── Demo data models ───────────────────────────────────────────────────────────

private data class DemoAlert(
    val severity: String,   // "critical" | "warning"
    val policy: String,
    val condition: String,
    val value: String,
    val triggeredAgo: String,
)

private data class DemoMetric(
    val label: String,
    val value: String,
    val unit: String,
    val trend: String?,
    val trendUp: Boolean?,
    val statusColor: Color,
    val sparkline: List<Float>,
)

// ── Hardcoded demo data ────────────────────────────────────────────────────────

private val demoAlerts = listOf(
    DemoAlert("critical", "Payment Service",  "High Error Rate",         "5.2%",    "12 min ago"),
    DemoAlert("critical", "Auth API Gateway", "Response Time > 2000ms",  "2,341ms", "34 min ago"),
    DemoAlert("warning",  "Database Pool",    "Connection Utilization",  "85%",     "1h 20m ago"),
    DemoAlert("warning",  "CDN Edge Cache",   "Cache Miss Rate High",    "42%",     "2h 05m ago"),
)

private val demoMetrics = listOf(
    DemoMetric("CPU Usage",      "67",   "%",    "↑ 3%",   true,  StatusWarning,
        listOf(42f, 55f, 48f, 60f, 58f, 72f, 67f, 65f, 70f, 67f)),
    DemoMetric("Memory",         "81",   "%",    "↑ 5%",   true,  StatusCritical,
        listOf(72f, 74f, 73f, 76f, 78f, 79f, 80f, 79f, 82f, 81f)),
    DemoMetric("Request Rate",   "2.4k", "/min", null,     null,  StatusHealthy,
        listOf(1.8f, 2.0f, 2.2f, 2.1f, 2.5f, 2.6f, 2.3f, 2.4f, 2.5f, 2.4f)),
    DemoMetric("Error Rate",     "0.8",  "%",    "↓ 0.2%", false, StatusHealthy,
        listOf(1.2f, 1.0f, 0.9f, 1.1f, 0.8f, 0.7f, 0.9f, 0.8f, 0.7f, 0.8f)),
    DemoMetric("P95 Latency",    "245",  "ms",   "↓ 12ms", false, StatusHealthy,
        listOf(280f, 265f, 270f, 258f, 262f, 250f, 248f, 255f, 240f, 245f)),
    DemoMetric("Active Conns.",  "1,247","",     "↑ 89",   true,  StatusHealthy,
        listOf(980f, 1050f, 1100f, 1080f, 1150f, 1200f, 1180f, 1230f, 1210f, 1247f)),
)

// ── Root screen ───────────────────────────────────────────────────────────────

@Composable
fun DemoPreviewScreen(onBackClick: () -> Unit = {}) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        item { DemoHeader(onBackClick = onBackClick) }

        // Alert summary banner
        item { AlertSummaryBanner() }

        // Service status
        item {
            Text(
                text = "Service Status",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        item { DemoServiceStatusRow() }

        // Active alerts section
        item {
            DemoSectionHeader(
                title = "Active Alerts (${demoAlerts.size})",
                icon = Icons.Default.Warning,
                iconTint = StatusCritical,
            )
        }
        items(demoAlerts) { alert -> DemoAlertCard(alert) }

        // Grafana metrics section
        item {
            DemoSectionHeader(
                title = "Grafana Metrics",
                icon = Icons.Default.ShowChart,
                iconTint = GrafanaOrange,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                demoMetrics.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { metric ->
                            DemoMetricCard(metric = metric, modifier = Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun DemoHeader(onBackClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Column {
                Text(
                    text = "Monitoring Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Live system overview",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DemoBadge()
    }
}

@Composable
private fun DemoBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    Box(
        modifier = Modifier
            .background(
                color = GrafanaOrange.copy(alpha = alpha * 0.15f),
                shape = RoundedCornerShape(8.dp),
            )
            .border(1.dp, GrafanaOrange.copy(alpha = alpha), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "DEMO",
            color = GrafanaOrange,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
        )
    }
}

// ── Alert summary banner ──────────────────────────────────────────────────────

@Composable
private fun AlertSummaryBanner() {
    val criticalCount = demoAlerts.count { it.severity == "critical" }
    val warningCount  = demoAlerts.count { it.severity == "warning" }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = StatusCritical.copy(alpha = 0.12f),
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = StatusCritical,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$criticalCount Critical · $warningCount Warning",
                    style = MaterialTheme.typography.titleSmall,
                    color = StatusCritical,
                )
                Text(
                    text = "Alerts require immediate attention",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Service status row ────────────────────────────────────────────────────────

@Composable
private fun DemoServiceStatusRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DemoServiceCard(
            name = "Grafana",
            subtitle = "Dashboards & Metrics",
            statusColor = StatusHealthy,
            statusLabel = "Connected",
            detail = "v10.2.1  ·  6 dashboards",
            brandColor = GrafanaOrange,
            modifier = Modifier.weight(1f),
        )
        DemoServiceCard(
            name = "New Relic",
            subtitle = "APM & Alerts",
            statusColor = StatusCritical,
            statusLabel = "2 Critical",
            detail = "8 apps monitored",
            brandColor = NewRelicGreen,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DemoServiceCard(
    name: String,
    subtitle: String,
    statusColor: Color,
    statusLabel: String,
    detail: String,
    brandColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceBright),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(brandColor, CircleShape),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(statusColor, CircleShape),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// ── Alert card ────────────────────────────────────────────────────────────────

@Composable
private fun DemoAlertCard(alert: DemoAlert) {
    val isCritical = alert.severity == "critical"
    val color = if (isCritical) StatusCritical else StatusWarning
    val label = if (isCritical) "CRITICAL" else "WARNING"

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceBright),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Severity stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .background(color, RoundedCornerShape(2.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                    Text(
                        text = alert.policy,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = alert.condition,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = alert.value,
                    style = MaterialTheme.typography.titleSmall,
                    color = color,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = alert.triggeredAgo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Metric card ───────────────────────────────────────────────────────────────

@Composable
private fun DemoMetricCard(metric: DemoMetric, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceBright),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = metric.statusColor,
                )
                if (metric.unit.isNotEmpty()) {
                    Text(
                        text = metric.unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp, start = 2.dp),
                    )
                }
            }
            // Sparkline chart
            Spacer(modifier = Modifier.height(6.dp))
            SparklineChart(
                data = metric.sparkline,
                lineColor = metric.statusColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
            )
            // Trend indicator
            metric.trend?.let { trend ->
                Spacer(modifier = Modifier.height(4.dp))
                val trendColor = when {
                    metric.trendUp == true && metric.statusColor == StatusHealthy -> StatusHealthy
                    metric.trendUp == true -> StatusWarning
                    else -> StatusHealthy
                }
                Text(
                    text = trend,
                    style = MaterialTheme.typography.labelSmall,
                    color = trendColor,
                )
            }
        }
    }
}

// ── Sparkline canvas ──────────────────────────────────────────────────────────

@Composable
private fun SparklineChart(
    data: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    if (data.size < 2) return
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val minVal = data.min()
        val maxVal = data.max()
        val range  = (maxVal - minVal).takeIf { it > 0f } ?: 1f
        val w = size.width
        val h = size.height
        val step = w / (data.size - 1)

        fun xAt(i: Int) = i * step
        fun yAt(v: Float) = h - ((v - minVal) / range) * h

        // Filled area under curve
        val fillPath = Path().apply {
            moveTo(xAt(0), h)
            lineTo(xAt(0), yAt(data[0]))
            data.forEachIndexed { i, v -> lineTo(xAt(i), yAt(v)) }
            lineTo(xAt(data.size - 1), h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
            ),
        )

        // Line
        val linePath = Path().apply {
            moveTo(xAt(0), yAt(data[0]))
            data.forEachIndexed { i, v -> lineTo(xAt(i), yAt(v)) }
        }
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx()),
        )

        // End dot
        drawCircle(
            color = lineColor,
            radius = 3.dp.toPx(),
            center = Offset(xAt(data.size - 1), yAt(data.last())),
        )
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun DemoSectionHeader(title: String, icon: ImageVector, iconTint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF111216)
@Composable
private fun DemoPreviewScreenPreview() {
    MaterialTheme {
        DemoPreviewScreen()
    }
}
