package com.monitoring.dashboard.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.monitoring.dashboard.MainActivity
import com.monitoring.dashboard.R
import com.monitoring.dashboard.ui.navigation.Screen
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun createNotificationChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val criticalChannel = NotificationChannel(
            CHANNEL_CRITICAL,
            context.getString(R.string.notification_channel_critical),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_critical_desc)
            enableVibration(true)
            enableLights(true)
        }

        val warningChannel = NotificationChannel(
            CHANNEL_WARNING,
            context.getString(R.string.notification_channel_warning),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_warning_desc)
        }

        manager.createNotificationChannel(criticalChannel)
        manager.createNotificationChannel(warningChannel)
        Timber.d("Notification channels created")
    }

    fun showAlertNotification(
        newViolationCount: Int,
        policyName: String?,
        isCritical: Boolean = true,
    ) {
        if (!hasNotificationPermission()) {
            Timber.w("POST_NOTIFICATIONS permission not granted – skipping notification")
            return
        }

        val channelId = if (isCritical) CHANNEL_CRITICAL else CHANNEL_WARNING

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(Screen.EXTRA_DESTINATION, Screen.DEST_ALERTS)
            data = Uri.parse("monitoring://alerts")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (newViolationCount == 1) {
            context.getString(R.string.notification_single_violation)
        } else {
            context.getString(R.string.notification_multiple_violations, newViolationCount)
        }
        val body = policyName ?: context.getString(R.string.notification_fallback_body)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (isCritical) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT,
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        Timber.d("Alert notification shown: $title")
    }

    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        const val CHANNEL_CRITICAL = "alert_violations_critical"
        const val CHANNEL_WARNING = "alert_violations_warning"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_OPEN_APP = 0
    }
}
