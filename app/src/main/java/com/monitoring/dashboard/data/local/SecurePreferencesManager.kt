package com.monitoring.dashboard.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.monitoring.dashboard.domain.model.NewRelicRegion
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sensitive configuration values (API keys, base URLs) using
 * [EncryptedSharedPreferences] backed by Android Keystore.
 *
 * Fail-closed: if encryption cannot be opened after a wipe/retry, an in-memory
 * store is used so API keys are never written plaintext to disk. The user must
 * re-enter credentials ([needsCredentialReset]).
 */
@Singleton
class SecurePreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    @Volatile
    private var credentialResetRequired: Boolean = false

    /** True when encrypted storage could not be opened; credentials must be re-entered. */
    fun needsCredentialReset(): Boolean = credentialResetRequired

    private val prefs: SharedPreferences by lazy {
        // Remove legacy plaintext fallback file from older builds
        context.deleteSharedPreferences("${PREFS_FILE_NAME}_fallback")
        try {
            createEncryptedPrefs()
        } catch (firstException: Exception) {
            Timber.w(firstException, "EncryptedSharedPreferences init failed – resetting prefs file")
            context.deleteSharedPreferences(PREFS_FILE_NAME)
            try {
                createEncryptedPrefs()
            } catch (secondException: Exception) {
                Timber.e(
                    secondException,
                    "EncryptedSharedPreferences failed twice – using in-memory store (no plaintext disk fallback)",
                )
                credentialResetRequired = true
                MemorySharedPreferences()
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
        prefs.edit().putString(KEY_GRAFANA_API_KEY, apiKey).commit()
    }

    fun getGrafanaApiKey(): String? =
        prefs.getString(KEY_GRAFANA_API_KEY, null)

    fun saveGrafanaBaseUrl(baseUrl: String) {
        prefs.edit().putString(KEY_GRAFANA_BASE_URL, baseUrl).commit()
    }

    fun getGrafanaBaseUrl(): String? =
        prefs.getString(KEY_GRAFANA_BASE_URL, null)



    fun saveNewRelicApiKey(apiKey: String) {
        prefs.edit().putString(KEY_NEWRELIC_API_KEY, apiKey).commit()
    }

    fun getNewRelicApiKey(): String? =
        prefs.getString(KEY_NEWRELIC_API_KEY, null)

    fun saveNewRelicAccountId(accountId: String) {
        prefs.edit().putString(KEY_NEWRELIC_ACCOUNT_ID, accountId).commit()
    }

    fun getNewRelicAccountId(): String? =
        prefs.getString(KEY_NEWRELIC_ACCOUNT_ID, null)

    fun saveNewRelicRegion(region: NewRelicRegion) {
        prefs.edit().putString(KEY_NEWRELIC_REGION, region.storageId).commit()
    }

    fun getNewRelicRegion(): NewRelicRegion =
        NewRelicRegion.fromStorage(prefs.getString(KEY_NEWRELIC_REGION, null))

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
        !credentialResetRequired &&
            (prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false) || isAnySourceConfigured())

    fun setOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).commit()
    }

    fun isAppLockEnabled(): Boolean =
        prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).commit()
    }

    fun getGithubToken(): String? =
        prefs.getString(KEY_GITHUB_TOKEN, null)

    fun saveGithubToken(token: String) {
        prefs.edit().putString(KEY_GITHUB_TOKEN, token).commit()
    }

    fun getGithubRepo(): String? =
        prefs.getString(KEY_GITHUB_REPO, null)

    fun saveGithubRepo(repo: String) {
        prefs.edit().putString(KEY_GITHUB_REPO, repo).commit()
    }

    /** Active environment profile id (default / staging / prod). */
    fun getActiveProfileId(): String =
        prefs.getString(KEY_ACTIVE_PROFILE, PROFILE_DEFAULT) ?: PROFILE_DEFAULT

    fun setActiveProfileId(profileId: String) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE, profileId).commit()
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
        prefs.edit().putString(KEY_PROFILE_IDS, ids.joinToString(",")).commit()
    }

    fun saveProfileCredentials(
        profileId: String,
        grafanaBaseUrl: String?,
        grafanaApiKey: String?,
        newRelicApiKey: String?,
        newRelicAccountId: String?,
        newRelicRegion: String? = getNewRelicRegion().storageId,
    ) {
        prefs.edit()
            .putString(profileKey(profileId, "grafana_url"), grafanaBaseUrl)
            .putString(profileKey(profileId, "grafana_key"), grafanaApiKey)
            .putString(profileKey(profileId, "nr_key"), newRelicApiKey)
            .putString(profileKey(profileId, "nr_account"), newRelicAccountId)
            .putString(profileKey(profileId, "nr_region"), newRelicRegion)
            .commit()
    }

    fun loadProfileIntoActive(profileId: String) {
        val url = prefs.getString(profileKey(profileId, "grafana_url"), null)
        val gKey = prefs.getString(profileKey(profileId, "grafana_key"), null)
        val nrKey = prefs.getString(profileKey(profileId, "nr_key"), null)
        val nrAccount = prefs.getString(profileKey(profileId, "nr_account"), null)
        val nrRegion = prefs.getString(profileKey(profileId, "nr_region"), null)
        prefs.edit().apply {
            if (url != null) putString(KEY_GRAFANA_BASE_URL, url)
            if (gKey != null) putString(KEY_GRAFANA_API_KEY, gKey)
            if (nrKey != null) putString(KEY_NEWRELIC_API_KEY, nrKey)
            if (nrAccount != null) putString(KEY_NEWRELIC_ACCOUNT_ID, nrAccount)
            if (nrRegion != null) putString(KEY_NEWRELIC_REGION, nrRegion)
            putString(KEY_ACTIVE_PROFILE, profileId)
        }.commit()
    }

    fun snapshotActiveIntoProfile(profileId: String) {
        saveProfileCredentials(
            profileId = profileId,
            grafanaBaseUrl = getGrafanaBaseUrl(),
            grafanaApiKey = getGrafanaApiKey(),
            newRelicApiKey = getNewRelicApiKey(),
            newRelicAccountId = getNewRelicAccountId(),
            newRelicRegion = getNewRelicRegion().storageId,
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
            .commit()
    }


    fun getLastKnownViolationIds(): Set<Long> {
        val raw = prefs.getString(KEY_LAST_VIOLATION_IDS, null) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    }



    fun clearAll() {
        // commit() so callers (Settings reset, instrumented tests) observe an
        // empty store immediately. apply() can race a following read.
        prefs.edit().clear().commit()
    }

    companion object {
        private const val PREFS_FILE_NAME = "monitoring_secure_prefs"
        private const val KEY_GRAFANA_API_KEY = "grafana_api_key"
        private const val KEY_GRAFANA_BASE_URL = "grafana_base_url"
        private const val KEY_NEWRELIC_API_KEY = "newrelic_api_key"
        private const val KEY_NEWRELIC_ACCOUNT_ID = "newrelic_account_id"
        private const val KEY_NEWRELIC_REGION = "newrelic_region"
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

