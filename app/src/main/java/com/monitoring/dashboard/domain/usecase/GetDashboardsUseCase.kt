package com.monitoring.dashboard.domain.usecase

import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.GrafanaRepository
import com.monitoring.dashboard.domain.model.Dashboard
import com.monitoring.dashboard.domain.model.toDomain
import javax.inject.Inject

class GetDashboardsUseCase @Inject constructor(
    private val repository: GrafanaRepository,
) {
    suspend operator fun invoke(
        query: String? = null,
        tag: String? = null,
        starred: Boolean? = null,
        limit: Int = 100,
    ): NetworkResult<List<Dashboard>> =
        repository.searchDashboards(
            query = query?.takeIf { it.isNotBlank() },
            type = "dash-db",
            tag = tag,
            starred = starred,
            limit = limit,
        ).map { list -> list.map { it.toDomain() } }
}
