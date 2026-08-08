package com.monitoring.dashboard.domain.usecase

import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.NewRelicRepository
import javax.inject.Inject

class GetNewRelicApplicationsUseCase @Inject constructor(
    private val repository: NewRelicRepository,
) {
    suspend operator fun invoke(filterName: String? = null): NetworkResult<List<NewRelicApplicationDto>> =
        repository.getApplications(filterName = filterName)
}
