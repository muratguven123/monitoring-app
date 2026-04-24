package com.monitoring.dashboard.ui.screen.dashboarddetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monitoring.dashboard.domain.model.DashboardDetail
import com.monitoring.dashboard.domain.model.Panel
import com.monitoring.dashboard.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardDetailScreen(
    uid: String,
    title: String,
    onBackClick: () -> Unit,
    viewModel: DashboardDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uid) {
        viewModel.loadDetail(uid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text     = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (uiState.detail != null) {
                        uiState.detail!!.refresh?.let { refresh ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text  = refresh,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->

        when {
            uiState.isLoading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = OrangeGrafana)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Paneller yükleniyor...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Error,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.error,
                            modifier           = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text  = uiState.errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.retry(uid) }) {
                            Text("Tekrar Dene")
                        }
                    }
                }
            }

            uiState.detail != null -> {
                DashboardDetailContent(
                    detail         = uiState.detail!!,
                    paddingValues  = paddingValues,
                )
            }
        }
    }
}

@Composable
private fun DashboardDetailContent(
    detail: DashboardDetail,
    paddingValues: PaddingValues,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start  = 16.dp,
            end    = 16.dp,
            top    = paddingValues.calculateTopPadding() + 8.dp,
            bottom = 16.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
    ) {

        // ── Dashboard metadata header ──────────────────────────────
        item(span = { GridItemSpan(2) }) {
            DashboardMetaRow(detail)
        }

        // ── Tag chips ──────────────────────────────────────────────
        if (detail.tags.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    detail.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text     = tag,
                                style    = MaterialTheme.typography.labelSmall,
                                color    = GreenOk,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        // ── Panel header ───────────────────────────────────────────
        item(span = { GridItemSpan(2) }) {
            Text(
                text     = "${detail.panels.size} panel",
                style    = MaterialTheme.typography.labelLarge,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }

        if (detail.panels.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Bu dashboardda panel bulunmuyor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ── Panel cards ────────────────────────────────────────────
        items(
            items = detail.panels,
            key   = { it.id },
            span  = { panel ->
                // Geniş paneller (w >= 16) tam satır kaplar
                if (panel.width >= 16) GridItemSpan(2) else GridItemSpan(1)
            },
        ) { panel ->
            PanelCard(panel)
        }
    }
}

@Composable
private fun DashboardMetaRow(detail: DashboardDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        detail.folderTitle?.let { folder ->
            Icon(
                imageVector        = Icons.Default.Folder,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text  = folder,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
        }

        detail.updatedBy?.let { updatedBy ->
            Icon(
                imageVector        = Icons.Default.Person,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text  = updatedBy,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PanelCard(panel: Panel) {
    val (accent, icon) = panelStyle(panel.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape    = RoundedCornerShape(6.dp),
                    color    = accent.copy(alpha = 0.15f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector        = icon,
                            contentDescription = null,
                            tint               = accent,
                            modifier           = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text     = panel.type,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = accent,
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text     = panel.title,
                style    = MaterialTheme.typography.titleSmall,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            panel.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = desc,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Placeholder chart area
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text  = "Grafana görselleştirmesi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Panel türüne göre renk ve ikon çifti döndürür.
 */
@Composable
private fun panelStyle(type: String): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.vector.ImageVector> {
    return when (type.lowercase()) {
        "stat"        -> Pair(PurpleStat,    Icons.Default.Numbers)
        "gauge"       -> Pair(OrangeGrafana, Icons.Default.Speed)
        "timeseries",
        "graph"       -> Pair(BlueInfo,      Icons.Default.ShowChart)
        "table"       -> Pair(GreenOk,       Icons.Default.TableChart)
        "barchart",
        "bargauge"    -> Pair(YellowWarning, Icons.Default.BarChart)
        "piechart"    -> Pair(OrangeGrafanaLight, Icons.Default.PieChart)
        "logs"        -> Pair(GreenOk,       Icons.Default.List)
        "alertlist"   -> Pair(RedError,      Icons.Default.Notifications)
        else          -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Dashboard)
    }
}
