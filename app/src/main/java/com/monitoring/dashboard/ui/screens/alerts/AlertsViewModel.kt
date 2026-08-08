package com.monitoring.dashboard.ui.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.domain.model.AlertViolation
import com.monitoring.dashboard.domain.usecase.GetOpenViolationsUseCase
import com.monitoring.dashboard.domain.usecase.SyncAlertSnapshotUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val isLoading: Boolean = true,
    val violations: List<AlertViolation> = emptyList(),
    val filtered: List<AlertViolation> = emptyList(),
    val filter: AlertFilter = AlertFilter.OPEN,
    val error: String? = null,
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val getOpenViolationsUseCase: GetOpenViolationsUseCase,
    private val syncAlertSnapshotUseCase: SyncAlertSnapshotUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getOpenViolationsUseCase.observe().collect { list ->
                _uiState.update {
                    val filter = it.filter
                    it.copy(
                        isLoading = false,
                        violations = list,
                        filtered = applyFilter(list, filter),
                    )
                }
            }
        }
        refresh()
    }

    fun setFilter(filter: AlertFilter) {
        _uiState.update {
            it.copy(filter = filter, filtered = applyFilter(it.violations, filter))
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = syncAlertSnapshotUseCase()) {
                is NetworkResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun muteForHours(id: Long, hours: Int) {
        viewModelScope.launch {
            val until = System.currentTimeMillis() + hours * 60L * 60L * 1000L
            userPreferencesRepository.muteViolation(id, until)
        }
    }

    private fun applyFilter(list: List<AlertViolation>, filter: AlertFilter): List<AlertViolation> =
        when (filter) {
            AlertFilter.ALL -> list
            AlertFilter.OPEN -> list.filter { it.isOpen }
            AlertFilter.CRITICAL -> list.filter {
                it.isOpen && it.severity.equals("critical", ignoreCase = true)
            }
            AlertFilter.RESOLVED -> list.filter { !it.isOpen }
        }
}
