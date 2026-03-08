package com.monitoring.dashboard.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.domain.usecase.CheckGrafanaHealthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val isLoading: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Idle,
)

sealed class ConnectionStatus {
    data object Idle    : ConnectionStatus()
    data object Checking : ConnectionStatus()
    data class  Success(val version: String) : ConnectionStatus()
    data class  Failure(val message: String) : ConnectionStatus()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securePreferencesManager: SecurePreferencesManager,
    private val checkGrafanaHealthUseCase: CheckGrafanaHealthUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Pre-fill saved values
        _uiState.update {
            it.copy(
                baseUrl = securePreferencesManager.getGrafanaBaseUrl() ?: "",
                apiKey  = securePreferencesManager.getGrafanaApiKey()  ?: "",
            )
        }
    }

    fun onBaseUrlChange(value: String) {
        _uiState.update { it.copy(baseUrl = value, connectionStatus = ConnectionStatus.Idle) }
    }

    fun onApiKeyChange(value: String) {
        _uiState.update { it.copy(apiKey = value, connectionStatus = ConnectionStatus.Idle) }
    }

    /**
     * Saves credentials and performs a health check against Grafana.
     * On success, calls [onSuccess] so the caller can navigate.
     */
    fun testAndSave(onSuccess: () -> Unit) {
        val url    = _uiState.value.baseUrl.trim()
        val apiKey = _uiState.value.apiKey.trim()

        if (url.isBlank() || apiKey.isBlank()) {
            _uiState.update {
                it.copy(connectionStatus = ConnectionStatus.Failure("URL ve API anahtarı boş olamaz"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(connectionStatus = ConnectionStatus.Checking) }

            // Persist before testing (DynamicBaseUrlInterceptor reads at request-time)
            securePreferencesManager.saveGrafanaBaseUrl(url)
            securePreferencesManager.saveGrafanaApiKey(apiKey)

            when (val result = checkGrafanaHealthUseCase()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            connectionStatus = ConnectionStatus.Success(
                                "Grafana ${result.data.version} — DB: ${result.data.database}"
                            )
                        )
                    }
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            connectionStatus = ConnectionStatus.Failure(
                                result.message ?: "Bağlantı başarısız (${result.code})"
                            )
                        )
                    }
                }
                else -> Unit
            }
        }
    }
}
