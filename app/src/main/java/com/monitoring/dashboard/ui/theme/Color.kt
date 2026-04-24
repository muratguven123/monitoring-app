package com.monitoring.dashboard.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand / Grafana-inspired palette ─────────────────────────────────────────
val OrangeGrafana   = Color(0xFFFF7C00)
val OrangeGrafanaLight = Color(0xFFFFAA4C)
val OrangeContainer = Color(0xFF3D2200)

val GrafanaOrange = OrangeGrafana
val NewRelicGreen = Color(0xFF00AC69)

// ── Dark surface hierarchy ────────────────────────────────────────────────────
val Background      = Color(0xFF111216)
val SurfaceDim      = Color(0xFF181B1F)
val Surface         = Color(0xFF1F2228)
val SurfaceBright   = Color(0xFF282D36)
val SurfaceVariant  = Color(0xFF2E3340)

// ── Content ───────────────────────────────────────────────────────────────────
val OnBackground    = Color(0xFFE4E6EB)
val OnSurface       = Color(0xFFCDD0D8)
val OnSurfaceVariant= Color(0xFF9DA3B0)
val Outline         = Color(0xFF3E4554)
val OutlineVariant  = Color(0xFF2C3140)

// ── Status colours ────────────────────────────────────────────────────────────
val GreenOk         = Color(0xFF73BF69)
val RedError        = Color(0xFFF2495C)
val YellowWarning   = Color(0xFFFFB357)
val BlueInfo        = Color(0xFF5794F2)
val GrayUnknown     = Color(0xFF8E8E8E)

val StatusHealthy = GreenOk
val StatusWarning = YellowWarning
val StatusCritical = RedError
val StatusGray = GrayUnknown

// ── Panel type accent colours ─────────────────────────────────────────────────
val PurpleStat      = Color(0xFFB877D9)
val TealTimeseries  = Color(0xFF37872D).copy(alpha = 0.8f)
