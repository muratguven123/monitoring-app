package com.monitoring.dashboard.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sensitive configuration values (API keys, base URLs) using
 * [EncryptedSharedPreferences] backed by Android Keystore.
 */
@Singleton
class SecurePreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // ── Grafana ────────────────────────────────────────────────────────────

    fun saveGrafanaApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GRAFANA_API_KEY, apiKey).apply()
    }

    fun getGrafanaApiKey(): String? =
        prefs.getString(KEY_GRAFANA_API_KEY, null)

    fun saveGrafanaBaseUrl(baseUrl: String) {
        prefs.edit().putString(KEY_GRAFANA_BASE_URL, baseUrl).apply()
    }

    fun getGrafanaBaseUrl(): String? =
        prefs.getString(KEY_GRAFANA_BASE_URL, null)

    // ── New Relic ──────────────────────────────────────────────────────────

    fun saveNewRelicApiKey(apiKey: String) {
        prefs.edit().putString(KEY_NEWRELIC_API_KEY, apiKey).apply()
    }

    fun getNewRelicApiKey(): String? =
        prefs.getString(KEY_NEWRELIC_API_KEY, null)

    fun saveNewRelicAccountId(accountId: String) {
        prefs.edit().putString(KEY_NEWRELIC_ACCOUNT_ID, accountId).apply()
    }

    fun getNewRelicAccountId(): String? =
        prefs.getString(KEY_NEWRELIC_ACCOUNT_ID, null)

    // ── Alert Violation Snapshot (for notification diffing) ───────────────

    /**
     * Persists the set of currently open violation IDs so that the next
     * [com.monitoring.dashboard.worker.AlertMonitorWorker] run can detect
     * newly appeared violations and fire a notification only for those.
     */
    fun saveLastKnownViolationIds(ids: Set<Long>) {
        // Serialise as a comma-separated string; empty set → empty string
        prefs.edit()
            .putString(KEY_LAST_VIOLATION_IDS, ids.joinToString(","))
            .apply()
    }

    /**
     * Returns the violation-ID snapshot saved by [saveLastKnownViolationIds],
     * or an empty set if no snapshot exists yet (first run).
     */
    fun getLastKnownViolationIds(): Set<Long> {
        val raw = prefs.getString(KEY_LAST_VIOLATION_IDS, null) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    }

    // ── General ────────────────────────────────────────────────────────────

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_FILE_NAME = "monitoring_secure_prefs"
        private const val KEY_GRAFANA_API_KEY = "grafana_api_key"
        private const val KEY_GRAFANA_BASE_URL = "grafana_base_url"
        private const val KEY_NEWRELIC_API_KEY = "newrelic_api_key"
        private const val KEY_NEWRELIC_ACCOUNT_ID = "newrelic_account_id"
        private const val KEY_LAST_VIOLATION_IDS = "last_known_violation_ids"
    }
}

