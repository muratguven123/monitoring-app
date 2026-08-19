package com.monitoring.dashboard.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.monitoring.dashboard.data.DataRefreshBus
import com.monitoring.dashboard.data.local.CacheInvalidator
import com.monitoring.dashboard.data.local.MetricThresholds
import com.monitoring.dashboard.data.local.NotificationPreferences
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.domain.model.GrafanaServerUrl
import com.monitoring.dashboard.domain.model.GrafanaUrlError
import com.monitoring.dashboard.domain.model.GrafanaUrlResult
import com.monitoring.dashboard.domain.model.NewRelicRegion
import com.monitoring.dashboard.domain.usecase.CheckGrafanaHealthUseCase
import com.monitoring.dashboard.domain.usecase.TestNewRelicConnectionUseCase
import com.monitoring.dashboard.ui.AppLockController
import com.monitoring.dashboard.worker.AlertMonitorWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SettingsUiState(
    val grafanaBaseUrl: String = "",
    val grafanaApiKey: String = "",
    val newRelicApiKey: String = "",
    val newRelicAccountId: String = "",
    val newRelicRegion: NewRelicRegion = NewRelicRegion.US,
    val githubToken: String = "",
    val githubRepo: String = "",
    val isSaved: Boolean = false,
    val saveError: Boolean = false,
    val isConnecting: Boolean = false,
    val connectionMessage: String? = null,
    val connectionSuccess: Boolean? = null,
    val activeProfileId: String = SecurePreferencesManager.PROFILE_DEFAULT,
    val profileIds: Set<String> = setOf(SecurePreferencesManager.PROFILE_DEFAULT),
    val appLockEnabled: Boolean = false,
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
    val metricThresholds: MetricThresholds = MetricThresholds(),
    val profileSwitchMessage: String? = null,
    /** Live validation of [grafanaBaseUrl] as the user types. */
    val grafanaUrlStatus: GrafanaUrlStatus = GrafanaUrlStatus.Empty,
)

/**
 * What to tell the user about the Grafana address they have typed.
 *
 * Feedback is immediate because a wrong address otherwise only surfaces much
 * later as a connection failure, where it is indistinguishable from the server
 * being down.
 */
sealed interface GrafanaUrlStatus {
    /** Nothing entered yet — show the hint, not an error. */
    data object Empty : GrafanaUrlStatus

    /** Usable address; [normalized] is what the app will actually call. */
    data class Valid(val normalized: String, val isCleartext: Boolean) : GrafanaUrlStatus

    data class Invalid(val error: GrafanaUrlError) : GrafanaUrlStatus
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    // Injected rather than resolved via WorkManager.getInstance(context): the
    // static lookup throws unless WorkManager has been initialised, which makes
    // this ViewModel untestable without static mocking. WorkManagerModule already
    // provides the same singleton.
    private val workManager: WorkManager,
    private val securePreferencesManager: SecurePreferencesManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val checkGrafanaHealthUseCase: CheckGrafanaHealthUseCase,
    private val testNewRelicConnectionUseCase: TestNewRelicConnectionUseCase,
    private val cacheInvalidator: CacheInvalidator,
    private val dataRefreshBus: DataRefreshBus,
    private val appLockController: AppLockController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val notificationPreferences = userPreferencesRepository.notificationPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationPreferences())

    val metricThresholds = userPreferencesRepository.metricThresholds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MetricThresholds())

    init {
        loadCurrentSettings()
        viewModelScope.launch {
            userPreferencesRepository.notificationPreferences.collect { prefs ->
                _uiState.update { it.copy(notificationPreferences = prefs) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.metricThresholds.collect { thresholds ->
                _uiState.update { it.copy(metricThresholds = thresholds) }
            }
        }
    }

    private fun loadCurrentSettings() {
        try {
            val storedGrafanaUrl = securePreferencesManager.getGrafanaBaseUrl() ?: ""
            _uiState.update {
                it.copy(
                    grafanaBaseUrl = storedGrafanaUrl,
                    grafanaUrlStatus = validateGrafanaUrl(storedGrafanaUrl),
                    grafanaApiKey = securePreferencesManager.getGrafanaApiKey() ?: "",
                    newRelicApiKey = securePreferencesManager.getNewRelicApiKey() ?: "",
                    newRelicAccountId = securePreferencesManager.getNewRelicAccountId() ?: "",
                    newRelicRegion = securePreferencesManager.getNewRelicRegion(),
                    githubToken = securePreferencesManager.getGithubToken() ?: "",
                    githubRepo = securePreferencesManager.getGithubRepo() ?: "",
                    isSaved = false,
                    activeProfileId = securePreferencesManager.getActiveProfileId(),
                    profileIds = securePreferencesManager.getProfileIds(),
                    appLockEnabled = securePreferencesManager.isAppLockEnabled(),
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load settings – prefs may have been reset")
        }
    }

    fun onGrafanaBaseUrlChanged(url: String) {
        _uiState.update {
            it.copy(
                grafanaBaseUrl = url,
                grafanaUrlStatus = validateGrafanaUrl(url),
                isSaved = false,
                connectionMessage = null,
            )
        }
    }

    private fun validateGrafanaUrl(raw: String): GrafanaUrlStatus =
        when (val result = GrafanaServerUrl.parse(raw)) {
            is GrafanaUrlResult.NotConfigured -> GrafanaUrlStatus.Empty
            is GrafanaUrlResult.Invalid -> GrafanaUrlStatus.Invalid(result.error)
            is GrafanaUrlResult.Valid -> GrafanaUrlStatus.Valid(
                normalized = result.url.baseUrl,
                isCleartext = result.url.isCleartext,
            )
        }

    fun onGrafanaApiKeyChanged(key: String) {
        _uiState.update { it.copy(grafanaApiKey = key, isSaved = false, connectionMessage = null) }
    }

    fun onNewRelicApiKeyChanged(key: String) {
        _uiState.update { it.copy(newRelicApiKey = key, isSaved = false, connectionMessage = null) }
    }

    fun onNewRelicAccountIdChanged(id: String) {
        _uiState.update { it.copy(newRelicAccountId = id, isSaved = false) }
    }

    fun onNewRelicRegionChanged(region: NewRelicRegion) {
        _uiState.update { it.copy(newRelicRegion = region, isSaved = false, connectionMessage = null) }
    }

    fun onGithubTokenChanged(token: String) {
        _uiState.update { it.copy(githubToken = token, isSaved = false) }
    }

    fun onGithubRepoChanged(repo: String) {
        _uiState.update { it.copy(githubRepo = repo, isSaved = false) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        securePreferencesManager.setAppLockEnabled(enabled)
        appLockController.onAppLockSettingChanged(enabled)
        _uiState.update { it.copy(appLockEnabled = enabled) }
    }

    fun setCriticalOnly(value: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setCriticalOnly(value) }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val prefs = _uiState.value.notificationPreferences
            userPreferencesRepository.setQuietHours(enabled, prefs.quietHoursStartHour, prefs.quietHoursEndHour)
        }
    }

    fun setPollIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setPollIntervalMinutes(minutes)
            AlertMonitorWorker.schedule(workManager, minutes.toLong())
        }
    }

    fun updateThresholds(thresholds: MetricThresholds) {
        viewModelScope.launch { userPreferencesRepository.updateThresholds(thresholds) }
    }

    fun clearProfileSwitchMessage() {
        _uiState.update { it.copy(profileSwitchMessage = null) }
    }

    fun switchProfile(profileId: String) {
        if (profileId == securePreferencesManager.getActiveProfileId()) return
        viewModelScope.launch {
            securePreferencesManager.snapshotActiveIntoProfile(securePreferencesManager.getActiveProfileId())
            val ids = securePreferencesManager.getProfileIds().toMutableSet().apply { add(profileId) }
            securePreferencesManager.saveProfileIds(ids)
            securePreferencesManager.loadProfileIntoActive(profileId)
            cacheInvalidator.clearAll()
            dataRefreshBus.requestRefresh()
            loadCurrentSettings()
            _uiState.update {
                it.copy(profileSwitchMessage = "Switched to $profileId — cache cleared")
            }
        }
    }

    fun saveCurrentAsProfile(profileId: String) {
        persistCredentials()
        securePreferencesManager.snapshotActiveIntoProfile(profileId)
        loadCurrentSettings()
        _uiState.update { it.copy(isSaved = true) }
    }

    /** Saves credentials then tests Grafana and/or New Relic connections. */
    fun connectAndSave() {
        viewModelScope.launch {
            val state = _uiState.value
            val grafanaUrlValid = state.grafanaUrlStatus is GrafanaUrlStatus.Valid
            val hasGrafana = grafanaUrlValid && state.grafanaApiKey.isNotBlank()
            val hasNewRelic = state.newRelicApiKey.isNotBlank()

            // Testing against an address we already know is unusable would just
            // produce a misleading "connection failed".
            if (state.grafanaUrlStatus is GrafanaUrlStatus.Invalid) {
                _uiState.update {
                    it.copy(
                        connectionSuccess = false,
                        connectionMessage = "Grafana: the server address is not valid",
                        isConnecting = false,
                    )
                }
                return@launch
            }

            if (!hasGrafana && !hasNewRelic) {
                _uiState.update {
                    it.copy(
                        connectionSuccess = false,
                        connectionMessage = "Configure at least one data source",
                        isConnecting = false,
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(isConnecting = true, connectionMessage = null, connectionSuccess = null, saveError = false)
            }

            // Persist first so interceptors pick up new credentials
            persistCredentials()

            val messages = mutableListOf<String>()
            var anySuccess = false
            var anyFailure = false

            if (hasGrafana) {
                when (val result = checkGrafanaHealthUseCase()) {
                    is NetworkResult.Success -> {
                        anySuccess = true
                        messages += "Grafana: connected (v${result.data.version})"
                    }
                    is NetworkResult.Error -> {
                        anyFailure = true
                        messages += "Grafana: ${result.message ?: "connection failed"}"
                    }
                    is NetworkResult.Loading -> Unit
                }
            }

            if (hasNewRelic) {
                when (val result = testNewRelicConnectionUseCase()) {
                    is NetworkResult.Success -> {
                        anySuccess = true
                        messages += "New Relic: connected (${result.data} apps)"
                    }
                    is NetworkResult.Error -> {
                        anyFailure = true
                        messages += "New Relic: ${result.message ?: "connection failed"}"
                    }
                    is NetworkResult.Loading -> Unit
                }
            }

            if (anySuccess) {
                securePreferencesManager.setOnboardingComplete(true)
                securePreferencesManager.snapshotActiveIntoProfile(securePreferencesManager.getActiveProfileId())
            }

            _uiState.update {
                it.copy(
                    isConnecting = false,
                    isSaved = anySuccess && !anyFailure,
                    connectionSuccess = anySuccess && !anyFailure,
                    connectionMessage = messages.joinToString("\n"),
                    saveError = anyFailure && !anySuccess,
                )
            }
        }
    }

    fun saveSettings() {
        try {
            persistCredentials()
            securePreferencesManager.setOnboardingComplete(true)
            securePreferencesManager.snapshotActiveIntoProfile(securePreferencesManager.getActiveProfileId())
            _uiState.update { it.copy(isSaved = true, saveError = false) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to save settings")
            _uiState.update { it.copy(isSaved = false, saveError = true) }
        }
    }

    private fun persistCredentials() {
        val state = _uiState.value
        if (state.grafanaBaseUrl.isNotBlank()) {
            // Store the normalised form ("grafana.example.com" →
            // "https://grafana.example.com/") so every consumer sees a canonical
            // address and does not have to re-parse user input.
            val normalized = GrafanaServerUrl.parse(state.grafanaBaseUrl).urlOrNull()?.baseUrl
            securePreferencesManager.saveGrafanaBaseUrl(normalized ?: state.grafanaBaseUrl.trim())
        }
        if (state.grafanaApiKey.isNotBlank()) {
            securePreferencesManager.saveGrafanaApiKey(state.grafanaApiKey.trim())
        }
        if (state.newRelicApiKey.isNotBlank()) {
            securePreferencesManager.saveNewRelicApiKey(state.newRelicApiKey.trim())
        }
        if (state.newRelicAccountId.isNotBlank()) {
            securePreferencesManager.saveNewRelicAccountId(state.newRelicAccountId.trim())
        }
        securePreferencesManager.saveNewRelicRegion(state.newRelicRegion)
        if (state.githubToken.isNotBlank()) {
            securePreferencesManager.saveGithubToken(state.githubToken.trim())
        }
        if (state.githubRepo.isNotBlank()) {
            securePreferencesManager.saveGithubRepo(state.githubRepo.trim())
        }
    }

    fun clearAllSettings() {
        try {
            securePreferencesManager.clearAll()
            appLockController.onAppLockSettingChanged(false)
            dataRefreshBus.requestRefresh()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear settings")
        }
        _uiState.update { SettingsUiState() }
    }
}
