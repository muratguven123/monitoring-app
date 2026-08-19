package com.monitoring.dashboard.fake

import com.monitoring.dashboard.data.remote.dto.newrelic.AlertViolationDto
import com.monitoring.dashboard.data.remote.dto.newrelic.MetricDataDto
import com.monitoring.dashboard.data.remote.dto.newrelic.MetricNameDto
import com.monitoring.dashboard.data.remote.dto.newrelic.NewRelicApplicationDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.NewRelicRepository

class FakeNewRelicRepository : NewRelicRepository {

    override suspend fun getApplications(
        filterName: String?,
    ): NetworkResult<List<NewRelicApplicationDto>> = NetworkResult.Success(
        listOf(
            NewRelicApplicationDto(
                id = 42L,
                name = "Fake App",
                language = "java",
                healthStatus = "green",
                reporting = true,
            ),
        ),
    )

    override suspend fun getApplicationById(id: Long): NetworkResult<NewRelicApplicationDto> =
        NetworkResult.Success(
            NewRelicApplicationDto(
                id = id,
                name = "Fake App",
                language = "java",
                healthStatus = "green",
                reporting = true,
            ),
        )

    override suspend fun getMetricNames(
        applicationId: Long,
        name: String?,
    ): NetworkResult<List<MetricNameDto>> = NetworkResult.Success(emptyList())

    override suspend fun getMetricData(
        applicationId: Long,
        names: List<String>,
        from: String?,
        to: String?,
        period: Int?,
        summarize: Boolean?,
    ): NetworkResult<MetricDataDto> = NetworkResult.Error(message = "not used in UI tests")

    override suspend fun getAlertViolations(
        onlyOpen: Boolean?,
    ): NetworkResult<List<AlertViolationDto>> = NetworkResult.Success(
        listOf(
            AlertViolationDto(
                id = 1L,
                label = "CPU High",
                policyName = "Prod policy",
                conditionName = "CPU",
                priority = "critical",
                openedAt = 1_000L,
                closedAt = null,
            ),
            AlertViolationDto(
                id = 2L,
                label = "Apdex Low",
                policyName = "Prod policy",
                conditionName = "Apdex",
                priority = "warning",
                openedAt = 2_000L,
                closedAt = null,
            ),
        ),
    )
}
