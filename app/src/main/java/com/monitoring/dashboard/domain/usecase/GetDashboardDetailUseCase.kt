package com.monitoring.dashboard.domain.usecase

import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.GrafanaRepository
import com.monitoring.dashboard.domain.model.DashboardDetail
import com.monitoring.dashboard.domain.model.toDomain
import javax.inject.Inject

class GetDashboardDetailUseCase @Inject constructor(
    private val repository: GrafanaRepository,
) {
    suspend operator fun invoke(uid: String): NetworkResult<DashboardDetail> =
        repository.getDashboardByUid(uid).map { it.toDomain() }
}
