package com.monitoring.dashboard.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.local.MetricThresholds
import com.monitoring.dashboard.data.local.NotificationPreferences
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.domain.usecase.CheckGrafanaHealthUseCase
import com.monitoring.dashboard.domain.usecase.TestNewRelicConnectionUseCase
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
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securePreferencesManager: SecurePreferencesManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val checkGrafanaHealthUseCase: CheckGrafanaHealthUseCase,
    private val testNewRelicConnectionUseCase: TestNewRelicConnectionUseCase,
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
            _uiState.update {
                it.copy(
                    grafanaBaseUrl = securePreferencesManager.getGrafanaBaseUrl() ?: "",
                    grafanaApiKey = securePreferencesManager.getGrafanaApiKey() ?: "",
                    newRelicApiKey = securePreferencesManager.getNewRelicApiKey() ?: "",
                    newRelicAccountId = securePreferencesManager.getNewRelicAccountId() ?: "",
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
        _uiState.update { it.copy(grafanaBaseUrl = url, isSaved = false, connectionMessage = null) }
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

    fun onGithubTokenChanged(token: String) {
        _uiState.update { it.copy(githubToken = token, isSaved = false) }
    }

    fun onGithubRepoChanged(repo: String) {
        _uiState.update { it.copy(githubRepo = repo, isSaved = false) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        securePreferencesManager.setAppLockEnabled(enabled)
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

    fun updateThresholds(thresholds: MetricThresholds) {
        viewModelScope.launch { userPreferencesRepository.updateThresholds(thresholds) }
    }

    fun switchProfile(profileId: String) {
        // Snapshot current into active profile, then load target
        securePreferencesManager.snapshotActiveIntoProfile(securePreferencesManager.getActiveProfileId())
        val ids = securePreferencesManager.getProfileIds().toMutableSet().apply { add(profileId) }
        securePreferencesManager.saveProfileIds(ids)
        securePreferencesManager.loadProfileIntoActive(profileId)
        loadCurrentSettings()
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
            val hasGrafana = state.grafanaBaseUrl.isNotBlank() && state.grafanaApiKey.isNotBlank()
            val hasNewRelic = state.newRelicApiKey.isNotBlank()

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
            securePreferencesManager.saveGrafanaBaseUrl(state.grafanaBaseUrl.trim())
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
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear settings")
        }
        _uiState.update { SettingsUiState() }
    }
}
