package com.monitoring.dashboard.data.repository

import com.monitoring.dashboard.data.remote.dto.DashboardDetailResponseDto
import com.monitoring.dashboard.data.remote.dto.DashboardSearchHitDto
import com.monitoring.dashboard.data.remote.dto.DatasourceDto
import com.monitoring.dashboard.data.remote.dto.GrafanaHealthDto
import com.monitoring.dashboard.data.remote.util.NetworkResult

/**
 * Repository abstraction for Grafana API operations.
 * All methods return [NetworkResult] wrappers for unified error handling.
 */
interface GrafanaRepository {

    // ── Dashboards ────────────────────────────────────────────────────────

    /**
     * Search/list dashboards with optional filters.
     */
    suspend fun searchDashboards(
        query: String? = null,
        type: String? = null,
        tag: String? = null,
        starred: Boolean? = null,
        limit: Int? = null,
        page: Int? = null,
    ): NetworkResult<List<DashboardSearchHitDto>>

    /**
     * Get full dashboard detail (with panels) by UID.
     */
    suspend fun getDashboardByUid(uid: String): NetworkResult<DashboardDetailResponseDto>

    // ── Datasources ───────────────────────────────────────────────────────

    /**
     * Get all configured datasources.
     */
    suspend fun getDatasources(): NetworkResult<List<DatasourceDto>>

    /**
     * Get a datasource by its numeric ID.
     */
    suspend fun getDatasourceById(id: Long): NetworkResult<DatasourceDto>

    /**
     * Get a datasource by its UID.
     */
    suspend fun getDatasourceByUid(uid: String): NetworkResult<DatasourceDto>

    // ── Health ─────────────────────────────────────────────────────────────

    /**
     * Health check — verify Grafana instance connectivity.
     */
    suspend fun getHealth(): NetworkResult<GrafanaHealthDto>
}

