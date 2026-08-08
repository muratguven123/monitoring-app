package com.monitoring.dashboard.domain.usecase

import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.NewRelicRepository
import javax.inject.Inject

/**
 * Verifies New Relic credentials by fetching the first page of applications.
 */
class TestNewRelicConnectionUseCase @Inject constructor(
    private val repository: NewRelicRepository,
) {
    suspend operator fun invoke(): NetworkResult<Int> {
        return when (val result = repository.getApplications()) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.size)
            is NetworkResult.Error -> NetworkResult.Error(
                code = result.code,
                message = result.message,
                exception = result.exception,
            )
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }
}
