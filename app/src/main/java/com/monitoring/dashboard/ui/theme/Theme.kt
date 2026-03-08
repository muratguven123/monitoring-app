package com.monitoring.dashboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary           = OrangeGrafana,
    onPrimary         = Background,
    primaryContainer  = OrangeContainer,
    onPrimaryContainer= OrangeGrafanaLight,

    background        = Background,
    onBackground      = OnBackground,

    surface           = Surface,
    surfaceDim        = SurfaceDim,
    surfaceBright     = SurfaceBright,
    surfaceVariant    = SurfaceVariant,
    onSurface         = OnSurface,
    onSurfaceVariant  = OnSurfaceVariant,

    outline           = Outline,
    outlineVariant    = OutlineVariant,

    error             = RedError,
    onError           = Background,
)

@Composable
fun MonitoringDashboardTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = MonitoringTypography,
        content     = content,
    )
}
