package com.monitoring.dashboard.ui.screen.dashboarddetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.domain.model.DashboardDetail
import com.monitoring.dashboard.domain.usecase.GetDashboardDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardDetailUiState(
    val isLoading: Boolean = true,
    val detail: DashboardDetail? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class DashboardDetailViewModel @Inject constructor(
    private val getDashboardDetailUseCase: GetDashboardDetailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardDetailUiState())
    val uiState: StateFlow<DashboardDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(uid: String) {
        if (_uiState.value.detail?.uid == uid) return // Zaten yüklü

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = getDashboardDetailUseCase(uid)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, detail = result.data)
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = result.message ?: "Dashboard yüklenemedi",
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    fun retry(uid: String) {
        _uiState.update { it.copy(detail = null) }
        loadDetail(uid)
    }
}
