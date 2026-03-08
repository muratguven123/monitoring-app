package com.monitoring.dashboard.domain.usecase

import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.GrafanaRepository
import com.monitoring.dashboard.domain.model.GrafanaHealth
import com.monitoring.dashboard.domain.model.toDomain
import javax.inject.Inject

class CheckGrafanaHealthUseCase @Inject constructor(
    private val repository: GrafanaRepository,
) {
    suspend operator fun invoke(): NetworkResult<GrafanaHealth> =
        repository.getHealth().map { it.toDomain() }
}
