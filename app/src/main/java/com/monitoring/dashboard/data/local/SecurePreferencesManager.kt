package com.monitoring.dashboard.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sensitive configuration values (API keys, base URLs) using
 * [EncryptedSharedPreferences] backed by Android Keystore.
 *
 * Robust against Keystore corruption: if the encrypted file cannot be opened
 * (e.g. after a PIN/biometric change, backup-restore, or partial write), the
 * file is deleted and a fresh empty store is created so the app never gets
 * stuck in a crash loop.  The user will simply have to re-enter their settings.
 */
@Singleton
class SecurePreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs: SharedPreferences by lazy {
        try {
            createEncryptedPrefs()
        } catch (firstException: Exception) {
            // Keystore key gone / file corrupted → wipe and retry
            Timber.w(firstException, "EncryptedSharedPreferences init failed – resetting prefs file")
            context.deleteSharedPreferences(PREFS_FILE_NAME)
            try {
                createEncryptedPrefs()
            } catch (secondException: Exception) {
                // Absolute last resort: plain (unencrypted) prefs so the app can at least open
                Timber.e(secondException, "EncryptedSharedPreferences failed twice – falling back to plain prefs")
                context.getSharedPreferences("${PREFS_FILE_NAME}_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }



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

    /** True when Grafana base URL and API key are both present. */
    fun isGrafanaConfigured(): Boolean =
        !getGrafanaBaseUrl().isNullOrBlank() && !getGrafanaApiKey().isNullOrBlank()

    /** True when New Relic API key is present (Account ID is optional until NerdGraph/NRQL). */
    fun isNewRelicConfigured(): Boolean =
        !getNewRelicApiKey().isNullOrBlank()

    /** True when at least one monitoring source is configured. */
    fun isAnySourceConfigured(): Boolean =
        isGrafanaConfigured() || isNewRelicConfigured()

    fun isOnboardingComplete(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false) || isAnySourceConfigured()

    fun setOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply()
    }

    fun isAppLockEnabled(): Boolean =
        prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    fun getGithubToken(): String? =
        prefs.getString(KEY_GITHUB_TOKEN, null)

    fun saveGithubToken(token: String) {
        prefs.edit().putString(KEY_GITHUB_TOKEN, token).apply()
    }

    fun getGithubRepo(): String? =
        prefs.getString(KEY_GITHUB_REPO, null)

    fun saveGithubRepo(repo: String) {
        prefs.edit().putString(KEY_GITHUB_REPO, repo).apply()
    }

    /** Active environment profile id (default / staging / prod). */
    fun getActiveProfileId(): String =
        prefs.getString(KEY_ACTIVE_PROFILE, PROFILE_DEFAULT) ?: PROFILE_DEFAULT

    fun setActiveProfileId(profileId: String) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE, profileId).apply()
    }

    fun getProfileIds(): Set<String> {
        val raw = prefs.getString(KEY_PROFILE_IDS, null)
        return if (raw.isNullOrBlank()) {
            setOf(PROFILE_DEFAULT)
        } else {
            raw.split(",").filter { it.isNotBlank() }.toSet().ifEmpty { setOf(PROFILE_DEFAULT) }
        }
    }

    fun saveProfileIds(ids: Set<String>) {
        prefs.edit().putString(KEY_PROFILE_IDS, ids.joinToString(",")).apply()
    }

    fun saveProfileCredentials(
        profileId: String,
        grafanaBaseUrl: String?,
        grafanaApiKey: String?,
        newRelicApiKey: String?,
        newRelicAccountId: String?,
    ) {
        prefs.edit()
            .putString(profileKey(profileId, "grafana_url"), grafanaBaseUrl)
            .putString(profileKey(profileId, "grafana_key"), grafanaApiKey)
            .putString(profileKey(profileId, "nr_key"), newRelicApiKey)
            .putString(profileKey(profileId, "nr_account"), newRelicAccountId)
            .apply()
    }

    fun loadProfileIntoActive(profileId: String) {
        val url = prefs.getString(profileKey(profileId, "grafana_url"), null)
        val gKey = prefs.getString(profileKey(profileId, "grafana_key"), null)
        val nrKey = prefs.getString(profileKey(profileId, "nr_key"), null)
        val nrAccount = prefs.getString(profileKey(profileId, "nr_account"), null)
        prefs.edit().apply {
            if (url != null) putString(KEY_GRAFANA_BASE_URL, url)
            if (gKey != null) putString(KEY_GRAFANA_API_KEY, gKey)
            if (nrKey != null) putString(KEY_NEWRELIC_API_KEY, nrKey)
            if (nrAccount != null) putString(KEY_NEWRELIC_ACCOUNT_ID, nrAccount)
            putString(KEY_ACTIVE_PROFILE, profileId)
        }.apply()
    }

    fun snapshotActiveIntoProfile(profileId: String) {
        saveProfileCredentials(
            profileId = profileId,
            grafanaBaseUrl = getGrafanaBaseUrl(),
            grafanaApiKey = getGrafanaApiKey(),
            newRelicApiKey = getNewRelicApiKey(),
            newRelicAccountId = getNewRelicAccountId(),
        )
        val ids = getProfileIds().toMutableSet().apply { add(profileId) }
        saveProfileIds(ids)
        setActiveProfileId(profileId)
    }

    private fun profileKey(profileId: String, field: String) = "profile_${profileId}_$field"

    // ── Alert Violation Snapshot (for notification diffing) ───────────────


    fun saveLastKnownViolationIds(ids: Set<Long>) {
        // Serialise as a comma-separated string; empty set → empty string
        prefs.edit()
            .putString(KEY_LAST_VIOLATION_IDS, ids.joinToString(","))
            .apply()
    }


    fun getLastKnownViolationIds(): Set<Long> {
        val raw = prefs.getString(KEY_LAST_VIOLATION_IDS, null) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    }



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
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_GITHUB_TOKEN = "github_token"
        private const val KEY_GITHUB_REPO = "github_repo"
        private const val KEY_ACTIVE_PROFILE = "active_profile"
        private const val KEY_PROFILE_IDS = "profile_ids"
        const val PROFILE_DEFAULT = "default"
        const val PROFILE_STAGING = "staging"
        const val PROFILE_PROD = "prod"
    }
}

