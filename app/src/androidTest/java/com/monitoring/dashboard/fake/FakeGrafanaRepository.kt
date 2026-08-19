package com.monitoring.dashboard.fake

import com.monitoring.dashboard.data.remote.dto.DashboardDetailResponseDto
import com.monitoring.dashboard.data.remote.dto.DashboardDto
import com.monitoring.dashboard.data.remote.dto.DashboardMetaDto
import com.monitoring.dashboard.data.remote.dto.DashboardSearchHitDto
import com.monitoring.dashboard.data.remote.dto.DatasourceDto
import com.monitoring.dashboard.data.remote.dto.GrafanaHealthDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.GrafanaRepository

class FakeGrafanaRepository : GrafanaRepository {

    override suspend fun searchDashboards(
        query: String?,
        type: String?,
        tag: String?,
        starred: Boolean?,
        limit: Int?,
        page: Int?,
    ): NetworkResult<List<DashboardSearchHitDto>> = NetworkResult.Success(
        listOf(
            DashboardSearchHitDto(
                id = 1L,
                uid = "fake-dash",
                title = "Fake Dashboard",
                url = "/d/fake-dash/fake-dashboard",
                type = "dash-db",
                tags = listOf("prod"),
            ),
        ),
    )

    override suspend fun getDashboardByUid(uid: String): NetworkResult<DashboardDetailResponseDto> =
        NetworkResult.Success(
            DashboardDetailResponseDto(
                meta = DashboardMetaDto(slug = uid, url = "/d/$uid"),
                dashboard = DashboardDto(id = 1L, uid = uid, title = "Fake Dashboard"),
            ),
        )

    override suspend fun getDatasources(): NetworkResult<List<DatasourceDto>> =
        NetworkResult.Success(emptyList())

    override suspend fun getDatasourceById(id: Long): NetworkResult<DatasourceDto> =
        NetworkResult.Error(code = 404, message = "not found")

    override suspend fun getDatasourceByUid(uid: String): NetworkResult<DatasourceDto> =
        NetworkResult.Error(code = 404, message = "not found")

    override suspend fun getHealth(): NetworkResult<GrafanaHealthDto> =
        NetworkResult.Success(
            GrafanaHealthDto(commit = "abc", database = "ok", version = "11.0.0"),
        )
}
