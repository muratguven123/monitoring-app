package com.monitoring.dashboard.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.monitoring.dashboard.R
import com.monitoring.dashboard.data.local.MonitoringDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.text.DateFormat
import java.util.Date

data class WidgetSnapshot(
    val openAlerts: Int,
    val healthy: Int,
    val warning: Int,
    val critical: Int,
    val lastSyncMs: Long?,
)

class MonitoringWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun database(): MonitoringDatabase
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
        val snapshot = runCatching {
            val db = entryPoint.database()
            val openAlerts = db.alertDao().getOpen().size
            val apps = db.newRelicDao().getAll()
            val healthy = apps.count { it.healthStatus.equals("green", true) }
            val warning = apps.count { it.healthStatus.equals("orange", true) }
            val critical = apps.count { it.healthStatus.equals("red", true) }
            val lastSync = listOfNotNull(
                apps.maxOfOrNull { it.cachedAt },
                db.alertDao().getAll().maxOfOrNull { it.cachedAt },
            ).maxOrNull()
            WidgetSnapshot(openAlerts, healthy, warning, critical, lastSync)
        }.getOrElse {
            WidgetSnapshot(0, 0, 0, 0, null)
        }

        provideContent {
            WidgetContent(snapshot = snapshot, context = context)
        }
    }
}

@Composable
private fun WidgetContent(snapshot: WidgetSnapshot, context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF1E1E1E)))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = context.getString(R.string.widget_title),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ColorProvider(Color.White),
            ),
        )
        Text(
            text = if (snapshot.openAlerts > 0) {
                context.getString(R.string.widget_alerts, snapshot.openAlerts)
            } else {
                context.getString(R.string.widget_no_data)
            },
            style = TextStyle(fontSize = 14.sp, color = ColorProvider(Color(0xFFB0B0B0))),
        )
        if (snapshot.healthy + snapshot.warning + snapshot.critical > 0) {
            Text(
                text = context.getString(
                    R.string.widget_health_summary,
                    snapshot.healthy,
                    snapshot.warning,
                    snapshot.critical,
                ),
                style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color(0xFFB0B0B0))),
            )
        }
        snapshot.lastSyncMs?.let { ms ->
            val formatted = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))
            Text(
                text = context.getString(R.string.widget_last_sync, formatted),
                style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(0xFF808080))),
            )
        }
    }
}

class MonitoringWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonitoringWidget()
}
