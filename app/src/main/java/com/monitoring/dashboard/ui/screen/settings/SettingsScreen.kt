@file:Suppress("unused")

package com.monitoring.dashboard.ui.screen.settings

import androidx.compose.runtime.Composable

/**
 * DEPRECATED: This file is kept only for compilation compatibility.
 * The canonical SettingsScreen lives in [com.monitoring.dashboard.ui.screens.settings].
 *
 * Legacy callers that pass [onSetupComplete] will have the callback ignored;
 * the new SettingsScreen uses in-screen save/clear actions instead.
 */
@Composable
fun SettingsScreen(
    onSetupComplete: () -> Unit = {},
) {
    // Delegate to the canonical implementation
    com.monitoring.dashboard.ui.screens.settings.SettingsScreen()
}
