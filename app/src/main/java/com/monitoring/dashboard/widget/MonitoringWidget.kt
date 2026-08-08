package com.monitoring.dashboard.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
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
        val openCount = runCatching {
            entryPoint.database().alertDao().getOpen().size
        }.getOrDefault(0)

        provideContent {
            WidgetContent(openCount = openCount, context = context)
        }
    }
}

@Composable
private fun WidgetContent(openCount: Int, context: Context) {
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
            text = if (openCount > 0) {
                context.getString(R.string.widget_alerts, openCount)
            } else {
                context.getString(R.string.widget_no_data)
            },
            style = TextStyle(
                fontSize = 14.sp,
                color = ColorProvider(Color(0xFFB0B0B0)),
            ),
        )
    }
}

class MonitoringWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonitoringWidget()
}
