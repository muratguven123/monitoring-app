package com.monitoring.dashboard.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class NotificationPreferences(
    val criticalOnly: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursEndHour: Int = 7,
    val pollIntervalMinutes: Int = 15,
)

data class MetricThresholds(
    val apdexGreen: Double = 0.90,
    val apdexYellow: Double = 0.70,
    val responseTimeGreenMs: Double = 500.0,
    val responseTimeYellowMs: Double = 2000.0,
    val errorRateGreen: Double = 1.0,
    val errorRateYellow: Double = 5.0,
    val pageLoadGreenMs: Double = 3000.0,
    val pageLoadYellowMs: Double = 7000.0,
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.dataStore

    val notificationPreferences: Flow<NotificationPreferences> = dataStore.data.map { prefs ->
        NotificationPreferences(
            criticalOnly = prefs[KEY_CRITICAL_ONLY] ?: false,
            quietHoursEnabled = prefs[KEY_QUIET_HOURS] ?: false,
            quietHoursStartHour = prefs[KEY_QUIET_START] ?: 22,
            quietHoursEndHour = prefs[KEY_QUIET_END] ?: 7,
            pollIntervalMinutes = (prefs[KEY_POLL_MINUTES] ?: 15).coerceAtLeast(15),
        )
    }

    val metricThresholds: Flow<MetricThresholds> = dataStore.data.map { prefs ->
        MetricThresholds(
            apdexGreen = prefs[KEY_APDEX_GREEN] ?: 0.90,
            apdexYellow = prefs[KEY_APDEX_YELLOW] ?: 0.70,
            responseTimeGreenMs = prefs[KEY_RT_GREEN] ?: 500.0,
            responseTimeYellowMs = prefs[KEY_RT_YELLOW] ?: 2000.0,
            errorRateGreen = prefs[KEY_ERR_GREEN] ?: 1.0,
            errorRateYellow = prefs[KEY_ERR_YELLOW] ?: 5.0,
            pageLoadGreenMs = prefs[KEY_PL_GREEN] ?: 3000.0,
            pageLoadYellowMs = prefs[KEY_PL_YELLOW] ?: 7000.0,
        )
    }

    val favoriteDashboardUids: Flow<Set<String>> = dataStore.data.map {
        it[KEY_FAV_DASHBOARDS] ?: emptySet()
    }

    val favoriteAppIds: Flow<Set<String>> = dataStore.data.map {
        it[KEY_FAV_APPS] ?: emptySet()
    }

    val mutedViolationUntil: Flow<Map<Long, Long>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_MUTED_VIOLATIONS] ?: emptySet()
        raw.mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val id = parts[0].toLongOrNull()
                val until = parts[1].toLongOrNull()
                if (id != null && until != null) id to until else null
            } else null
        }.toMap()
    }

    suspend fun setCriticalOnly(value: Boolean) {
        dataStore.edit { it[KEY_CRITICAL_ONLY] = value }
    }

    suspend fun setQuietHours(enabled: Boolean, startHour: Int = 22, endHour: Int = 7) {
        dataStore.edit {
            it[KEY_QUIET_HOURS] = enabled
            it[KEY_QUIET_START] = startHour
            it[KEY_QUIET_END] = endHour
        }
    }

    suspend fun setPollIntervalMinutes(minutes: Int) {
        dataStore.edit { it[KEY_POLL_MINUTES] = minutes.coerceAtLeast(15) }
    }

    suspend fun updateThresholds(thresholds: MetricThresholds) {
        dataStore.edit {
            it[KEY_APDEX_GREEN] = thresholds.apdexGreen
            it[KEY_APDEX_YELLOW] = thresholds.apdexYellow
            it[KEY_RT_GREEN] = thresholds.responseTimeGreenMs
            it[KEY_RT_YELLOW] = thresholds.responseTimeYellowMs
            it[KEY_ERR_GREEN] = thresholds.errorRateGreen
            it[KEY_ERR_YELLOW] = thresholds.errorRateYellow
            it[KEY_PL_GREEN] = thresholds.pageLoadGreenMs
            it[KEY_PL_YELLOW] = thresholds.pageLoadYellowMs
        }
    }

    suspend fun toggleFavoriteDashboard(uid: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_FAV_DASHBOARDS]?.toMutableSet() ?: mutableSetOf()
            if (!current.add(uid)) current.remove(uid)
            prefs[KEY_FAV_DASHBOARDS] = current
        }
    }

    suspend fun toggleFavoriteApp(appId: Long) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_FAV_APPS]?.toMutableSet() ?: mutableSetOf()
            val key = appId.toString()
            if (!current.add(key)) current.remove(key)
            prefs[KEY_FAV_APPS] = current
        }
    }

    suspend fun muteViolation(id: Long, untilEpochMs: Long) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_MUTED_VIOLATIONS]?.toMutableSet() ?: mutableSetOf()
            current.removeAll { it.startsWith("$id:") }
            current.add("$id:$untilEpochMs")
            prefs[KEY_MUTED_VIOLATIONS] = current
        }
    }

    suspend fun clearExpiredMutes(now: Long = System.currentTimeMillis()) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_MUTED_VIOLATIONS] ?: emptySet()
            prefs[KEY_MUTED_VIOLATIONS] = current.filter { entry ->
                val until = entry.substringAfter(":", "").toLongOrNull() ?: return@filter false
                until > now
            }.toSet()
        }
    }

    companion object {
        private val KEY_CRITICAL_ONLY = booleanPreferencesKey("critical_only")
        private val KEY_QUIET_HOURS = booleanPreferencesKey("quiet_hours")
        private val KEY_QUIET_START = intPreferencesKey("quiet_start")
        private val KEY_QUIET_END = intPreferencesKey("quiet_end")
        private val KEY_POLL_MINUTES = intPreferencesKey("poll_minutes")
        private val KEY_APDEX_GREEN = doublePreferencesKey("apdex_green")
        private val KEY_APDEX_YELLOW = doublePreferencesKey("apdex_yellow")
        private val KEY_RT_GREEN = doublePreferencesKey("rt_green")
        private val KEY_RT_YELLOW = doublePreferencesKey("rt_yellow")
        private val KEY_ERR_GREEN = doublePreferencesKey("err_green")
        private val KEY_ERR_YELLOW = doublePreferencesKey("err_yellow")
        private val KEY_PL_GREEN = doublePreferencesKey("pl_green")
        private val KEY_PL_YELLOW = doublePreferencesKey("pl_yellow")
        private val KEY_FAV_DASHBOARDS = stringSetPreferencesKey("fav_dashboards")
        private val KEY_FAV_APPS = stringSetPreferencesKey("fav_apps")
        private val KEY_MUTED_VIOLATIONS = stringSetPreferencesKey("muted_violations")
    }
}
