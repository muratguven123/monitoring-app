package com.monitoring.dashboard.ui.screens.grafana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.domain.model.Dashboard
import com.monitoring.dashboard.domain.usecase.GetDashboardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GrafanaDashboardsUiState(
    val isLoading: Boolean = true,
    val dashboards: List<Dashboard> = emptyList(),
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val page: Int = 1,
    val canLoadMore: Boolean = false,
    val favoriteUids: Set<String> = emptySet(),
)

@HiltViewModel
class GrafanaDashboardsViewModel @Inject constructor(
    private val getDashboardsUseCase: GetDashboardsUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GrafanaDashboardsUiState())
    val uiState: StateFlow<GrafanaDashboardsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.favoriteDashboardUids.collect { favs ->
                _uiState.update { it.copy(favoriteUids = favs) }
            }
        }
        loadDashboards(reset = true)
    }

    fun loadDashboards(reset: Boolean = true) {
        _uiState.update {
            it.copy(
                isLoading = reset,
                errorMessage = null,
                page = if (reset) 1 else it.page,
            )
        }
        viewModelScope.launch {
            val query = _uiState.value.searchQuery.ifBlank { null }
            when (
                val result = getDashboardsUseCase(
                    query = query,
                    limit = PAGE_SIZE,
                )
            ) {
                is NetworkResult.Success -> _uiState.update {
                    val merged = if (reset) result.data else it.dashboards + result.data
                    it.copy(
                        isLoading = false,
                        dashboards = merged.distinctBy { d -> d.uid },
                        errorMessage = null,
                        canLoadMore = result.data.size >= PAGE_SIZE,
                    )
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message ?: "Failed to load dashboards")
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun loadMore() {
        _uiState.update { it.copy(page = it.page + 1) }
        loadDashboards(reset = false)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadDashboards(reset = true)
    }

    fun toggleFavorite(uid: String) {
        viewModelScope.launch { userPreferencesRepository.toggleFavoriteDashboard(uid) }
    }

    companion object {
        private const val PAGE_SIZE = 50
    }
}
