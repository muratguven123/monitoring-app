package com.monitoring.dashboard.ui.screens.newrelic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer

/**
 * Tıklanabilir metric kart — başlık + mini Vico grafiği + "detaya git" oku.
 */
@Composable
fun MetricChartCard(
    chartData: MetricChartData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Başlık satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text  = chartData.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (chartData.unit.isNotBlank()) {
                    Text(
                        text  = chartData.unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Detay",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                chartData.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
                chartData.points.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = "Veri yok",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    MetricLineChart(
                        points   = chartData.points,
                        unit     = chartData.unit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    // Son değer
                    chartData.points.lastOrNull()?.let { last ->
                        Spacer(modifier = Modifier.height(4.dp))
                        val formatted = if (chartData.unit.isBlank())
                            "%.2f".format(last)
                        else
                            "%.2f ${chartData.unit}".format(last)
                        Text(
                            text  = "Şu an: $formatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Vico çizgi grafiği composable'ı — hem mini kartlarda hem detay ekranında kullanılır.
 */
@Composable
fun MetricLineChart(
    points: List<Float>,
    unit: String,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(points) {
        modelProducer.runTransaction {
            lineSeries { series(points) }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    rememberLine(
                        fill = remember { LineCartesianLayer.LineFill.single(fill(androidx.compose.ui.graphics.Color(0xFF4CAF50))) },
                    )
                )
            ),
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}
