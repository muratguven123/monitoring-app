package com.monitoring.dashboard.ui.screens.grafana

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.monitoring.dashboard.R
import com.monitoring.dashboard.data.remote.dto.PanelDto
import com.monitoring.dashboard.ui.components.ErrorMessage
import com.monitoring.dashboard.ui.components.LoadingIndicator

@Composable
fun GrafanaDashboardDetailScreen(
    onBackClick: () -> Unit,
    onPanelClick: (uid: String, panelId: Long, slug: String, panelTitle: String) -> Unit = { _, _, _, _ -> },
    viewModel: GrafanaDashboardDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imageLoader = viewModel.imageLoader

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(
                text = uiState.dashboard?.dashboard?.title
                    ?: stringResource(R.string.screen_grafana_detail_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.errorMessage != null -> ErrorMessage(
                message = uiState.errorMessage!!,
                onRetry = viewModel::loadDashboard,
            )
            uiState.dashboard != null -> {
                val dashboard = uiState.dashboard!!.dashboard
                val meta = uiState.dashboard!!.meta
                val baseUrl = uiState.grafanaBaseUrl
                val uid = dashboard.uid
                val slug = meta.slug.ifBlank { dashboard.title.lowercase().replace(" ", "-") }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Dashboard meta info
                    item {
                        Column {
                            if (dashboard.tags.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    dashboard.tags.forEach { tag ->
                                        AssistChip(onClick = {}, label = { Text(tag) })
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row {
                                meta.folderTitle?.let {
                                    Text(
                                        text = stringResource(R.string.grafana_folder_label, it),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                meta.updated?.let {
                                    Text(
                                        text = stringResource(R.string.grafana_updated_label, it),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    // Panel count header
                    val visiblePanels = dashboard.panels.filter { it.type != "row" && it.id != 0L }
                    item {
                        Text(
                            text = stringResource(R.string.grafana_panels_count, visiblePanels.size),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    // Panels with chart images
                    items(visiblePanels) { panel ->
                        PanelCard(
                            panel = panel,
                            renderUrl = buildRenderUrl(
                                baseUrl = baseUrl,
                                uid = uid,
                                slug = slug,
                                panelId = panel.id,
                            ),
                            imageLoader = imageLoader,
                            onClick = {
                                onPanelClick(uid, panel.id, slug, panel.title.ifBlank { "Panel #${panel.id}" })
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Builds the Grafana panel render URL.
 *
 * Example:
 *   https://host.grafana.net/render/d-solo/abc123/my-dashboard
 *     ?orgId=1&panelId=2&width=600&height=300&from=now-3h&to=now&theme=dark
 */
private fun buildRenderUrl(
    baseUrl: String,
    uid: String,
    slug: String,
    panelId: Long,
): String {
    if (baseUrl.isBlank() || uid.isBlank()) return ""
    return "$baseUrl/render/d-solo/$uid/$slug" +
        "?orgId=1" +
        "&panelId=$panelId" +
        "&width=600" +
        "&height=300" +
        "&from=now-3h" +
        "&to=now" +
        "&theme=dark"
}

@Composable
private fun PanelCard(
    panel: PanelDto,
    renderUrl: String,
    imageLoader: ImageLoader,
    onClick: () -> Unit = {},
) {
    val panelIcon = when (panel.type) {
        "graph", "timeseries" -> Icons.Default.BarChart
        "table"               -> Icons.Default.TableChart
        else                  -> Icons.Default.Dashboard
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Panel chart image
            if (renderUrl.isNotBlank()) {
                PanelChartImage(
                    url = renderUrl,
                    contentDescription = panel.title,
                    imageLoader = imageLoader,
                )
            }

            // Panel info row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = panelIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = panel.title.ifBlank { "Panel #${panel.id}" },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = panel.type.replaceFirstChar { it.uppercase() }.ifBlank { "Unknown" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    panel.description?.takeIf { it.isNotBlank() }?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Loads a Grafana rendered panel image using Coil.
 * Shows a shimmer placeholder while loading and a broken-image icon on error.
 */
@Composable
private fun PanelChartImage(
    url: String,
    contentDescription: String,
    imageLoader: ImageLoader,
) {
    val context = LocalContext.current

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f)          // 2:1 ratio — nice landscape chart shape
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp,
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Grafik yüklenemedi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
