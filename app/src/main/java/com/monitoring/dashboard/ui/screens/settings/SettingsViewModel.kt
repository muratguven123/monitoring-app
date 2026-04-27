package com.monitoring.dashboard.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

data class SettingsUiState(
    val grafanaBaseUrl: String = "",
    val grafanaApiKey: String = "",
    val newRelicApiKey: String = "",
    val newRelicAccountId: String = "",
    val isSaved: Boolean = false,
    val saveError: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securePreferencesManager: SecurePreferencesManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCurrentSettings()
    }

    private fun loadCurrentSettings() {
        try {
            _uiState.update {
                it.copy(
                    grafanaBaseUrl = securePreferencesManager.getGrafanaBaseUrl() ?: "",
                    grafanaApiKey = securePreferencesManager.getGrafanaApiKey() ?: "",
                    newRelicApiKey = securePreferencesManager.getNewRelicApiKey() ?: "",
                    newRelicAccountId = securePreferencesManager.getNewRelicAccountId() ?: "",
                    isSaved = false,
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load settings – prefs may have been reset")
            // Leave the UI in its default empty state; user will need to re-enter credentials
        }
    }

    fun onGrafanaBaseUrlChanged(url: String) {
        _uiState.update { it.copy(grafanaBaseUrl = url, isSaved = false) }
    }

    fun onGrafanaApiKeyChanged(key: String) {
        _uiState.update { it.copy(grafanaApiKey = key, isSaved = false) }
    }

    fun onNewRelicApiKeyChanged(key: String) {
        _uiState.update { it.copy(newRelicApiKey = key, isSaved = false) }
    }

    fun onNewRelicAccountIdChanged(id: String) {
        _uiState.update { it.copy(newRelicAccountId = id, isSaved = false) }
    }

    fun saveSettings() {
        try {
            val state = _uiState.value
            if (state.grafanaBaseUrl.isNotBlank()) {
                securePreferencesManager.saveGrafanaBaseUrl(state.grafanaBaseUrl)
            }
            if (state.grafanaApiKey.isNotBlank()) {
                securePreferencesManager.saveGrafanaApiKey(state.grafanaApiKey)
            }
            if (state.newRelicApiKey.isNotBlank()) {
                securePreferencesManager.saveNewRelicApiKey(state.newRelicApiKey)
            }
            if (state.newRelicAccountId.isNotBlank()) {
                securePreferencesManager.saveNewRelicAccountId(state.newRelicAccountId)
            }
            _uiState.update { it.copy(isSaved = true) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to save settings")
            // Surface the error in UI state so the user sees something went wrong
            _uiState.update { it.copy(isSaved = false, saveError = true) }
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
