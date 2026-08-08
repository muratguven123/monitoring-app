package com.monitoring.dashboard.domain.usecase

import com.monitoring.dashboard.data.local.NotificationPreferences
import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.domain.model.AlertViolation
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class ShouldNotifyViolationUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(violations: List<AlertViolation>): List<AlertViolation> {
        if (violations.isEmpty()) return emptyList()

        val prefs = userPreferencesRepository.notificationPreferences.first()
        userPreferencesRepository.clearExpiredMutes()
        val muted = userPreferencesRepository.mutedViolationUntil.first()
        val now = System.currentTimeMillis()

        if (isInQuietHours(prefs)) return emptyList()

        return violations.filter { violation ->
            val muteUntil = muted[violation.id]
            if (muteUntil != null && muteUntil > now) return@filter false
            if (prefs.criticalOnly) {
                violation.severity?.equals("critical", ignoreCase = true) == true
            } else {
                true
            }
        }
    }

    private fun isInQuietHours(prefs: NotificationPreferences): Boolean {
        if (!prefs.quietHoursEnabled) return false
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val start = prefs.quietHoursStartHour
        val end = prefs.quietHoursEndHour
        return if (start <= end) {
            hour in start until end
        } else {
            // Overnight window e.g. 22 → 7
            hour >= start || hour < end
        }
    }
}
