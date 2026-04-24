package com.monitoring.dashboard.ui.screen.dashboardlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.domain.model.Dashboard
import com.monitoring.dashboard.domain.usecase.GetDashboardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardListUiState(
    val isLoading: Boolean = false,
    val dashboards: List<Dashboard> = emptyList(),
    val filteredDashboards: List<Dashboard> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class DashboardListViewModel @Inject constructor(
    private val getDashboardsUseCase: GetDashboardsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardListUiState())
    val uiState: StateFlow<DashboardListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        // Debounce arama: 300ms bekle, sonra filtrele
        _searchQuery
            .debounce(300L)
            .onEach { query -> applyFilter(query) }
            .launchIn(viewModelScope)

        loadDashboards()
    }

    fun loadDashboards(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true, errorMessage = null)
                else it.copy(isLoading = true, errorMessage = null)
            }

            when (val result = getDashboardsUseCase()) {
                is NetworkResult.Success -> {
                    val sorted = result.data.sortedWith(
                        compareBy({ it.folderTitle ?: "" }, { it.title })
                    )
                    _uiState.update {
                        it.copy(
                            isLoading     = false,
                            isRefreshing  = false,
                            dashboards    = sorted,
                            filteredDashboards = applyQueryFilter(sorted, it.searchQuery),
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            isRefreshing = false,
                            errorMessage = result.message ?: "Bilinmeyen bir hata oluştu",
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    fun clearSearch() = onSearchQueryChange("")

    private fun applyFilter(query: String) {
        _uiState.update {
            it.copy(filteredDashboards = applyQueryFilter(it.dashboards, query))
        }
    }

    private fun applyQueryFilter(
        dashboards: List<Dashboard>,
        query: String,
    ): List<Dashboard> {
        if (query.isBlank()) return dashboards
        val q = query.lowercase()
        return dashboards.filter { d ->
            d.title.lowercase().contains(q) ||
            d.tags.any { it.lowercase().contains(q) } ||
            d.folderTitle?.lowercase()?.contains(q) == true
        }
    }
}
