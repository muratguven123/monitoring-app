package com.monitoring.dashboard.ui.screen.dashboardlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.monitoring.dashboard.domain.model.Dashboard
import com.monitoring.dashboard.ui.theme.GreenOk
import com.monitoring.dashboard.ui.theme.OrangeGrafana
import com.monitoring.dashboard.ui.theme.YellowWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardListScreen(
    onDashboardClick: (uid: String, title: String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: DashboardListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = "Dashboardlar",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboards(isRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {

            // ── Search Bar ────────────────────────────────────────────────
            OutlinedTextField(
                value         = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder   = { Text("Dashboard ara...") },
                leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon  = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle")
                        }
                    }
                },
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape         = RoundedCornerShape(12.dp),
            )

            // ── Content ───────────────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = OrangeGrafana)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Dashboardlar yükleniyor...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                uiState.errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                            OutlinedButton(onClick = { viewModel.loadDashboards() }) {
                                Text("Tekrar Dene")
                            }
                        }
                    }
                }

                uiState.filteredDashboards.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text  = "\"${uiState.searchQuery}\" için sonuç bulunamadı",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    SwipeRefresh(
                        state   = rememberSwipeRefreshState(uiState.isRefreshing),
                        onRefresh = { viewModel.loadDashboards(isRefresh = true) },
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                horizontal = 16.dp,
                                vertical   = 8.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Group by folder
                            val grouped = uiState.filteredDashboards
                                .groupBy { it.folderTitle ?: "Genel" }

                            grouped.forEach { (folderTitle, dashboards) ->
                                item {
                                    Text(
                                        text     = folderTitle,
                                        style    = MaterialTheme.typography.labelLarge,
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                    )
                                }
                                items(
                                    items = dashboards,
                                    key   = { it.uid },
                                ) { dashboard ->
                                    DashboardCard(
                                        dashboard    = dashboard,
                                        onClick      = { onDashboardClick(dashboard.uid, dashboard.title) },
                                    )
                                }
                            }

                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(
    dashboard: Dashboard,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Dashboard icon
            Surface(
                modifier = Modifier.size(40.dp),
                shape    = RoundedCornerShape(8.dp),
                color    = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector        = Icons.Default.Dashboard,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier           = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text     = dashboard.title,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (dashboard.tags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        dashboard.tags.take(3).forEach { tag ->
                            TagChip(tag)
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Starred indicator
            if (dashboard.isStarred) {
                Icon(
                    imageVector        = Icons.Default.Star,
                    contentDescription = "Favori",
                    tint               = YellowWarning,
                    modifier           = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
            }

            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TagChip(tag: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text     = tag,
            style    = MaterialTheme.typography.labelSmall,
            color    = GreenOk,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
