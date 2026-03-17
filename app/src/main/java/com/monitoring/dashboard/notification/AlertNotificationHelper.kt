package com.monitoring.dashboard.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.monitoring.dashboard.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages notification channel creation and alert violation notifications.
 *
 * Two channels are provided:
 *  - [CHANNEL_CRITICAL] – High importance, for "critical" priority violations.
 *  - [CHANNEL_WARNING]  – Default importance, for "warning" priority violations.
 */
@Singleton
class AlertNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    // ── Channel Setup ──────────────────────────────────────────────────────

    fun createNotificationChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val criticalChannel = NotificationChannel(
            CHANNEL_CRITICAL,
            "Critical Alert Violations",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications for critical-priority New Relic alert violations"
            enableVibration(true)
            enableLights(true)
        }

        val warningChannel = NotificationChannel(
            CHANNEL_WARNING,
            "Warning Alert Violations",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifications for warning-priority New Relic alert violations"
        }

        manager.createNotificationChannel(criticalChannel)
        manager.createNotificationChannel(warningChannel)
        Timber.d("Notification channels created")
    }

    // ── Show Notification ──────────────────────────────────────────────────

    /**
     * Shows a notification summarising [newViolationCount] new violations.
     *
     * @param newViolationCount  Number of newly detected violations.
     * @param policyName         Name of the first violation's policy (used as subtitle).
     * @param isCritical         Whether to use the high-importance critical channel.
     */
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (newViolationCount == 1) {
            "⚠ New Alert Violation"
        } else {
            "⚠ $newViolationCount New Alert Violations"
        }
        val body = policyName ?: "Open the app to review your alerts"

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

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun hasNotificationPermission(): Boolean {
        // POST_NOTIFICATIONS is only required from Android 13 (API 33)
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
        const val CHANNEL_WARNING  = "alert_violations_warning"
        private const val NOTIFICATION_ID      = 1001
        private const val REQUEST_CODE_OPEN_APP = 0
    }
}
