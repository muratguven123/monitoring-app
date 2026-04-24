package com.monitoring.dashboard.ui.screens.newrelic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.NewRelicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewRelicAppsUiState(
    val isLoading: Boolean = true,
    val applications: List<NewRelicApplicationDto> = emptyList(),
    val errorMessage: String? = null,
    val searchQuery: String = "",
)

@HiltViewModel
class NewRelicAppsViewModel @Inject constructor(
    private val newRelicRepository: NewRelicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewRelicAppsUiState())
    val uiState: StateFlow<NewRelicAppsUiState> = _uiState.asStateFlow()

    init {
        loadApplications()
    }

    fun loadApplications() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val filterName = _uiState.value.searchQuery.ifBlank { null }
            when (val result = newRelicRepository.getApplications(filterName = filterName)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, applications = result.data, errorMessage = null)
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message ?: "Failed to load applications")
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadApplications()
    }
}
