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
    }
}

