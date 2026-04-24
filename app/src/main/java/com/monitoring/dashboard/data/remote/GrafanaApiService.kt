package com.monitoring.dashboard.data.remote

import com.monitoring.dashboard.data.remote.dto.DashboardDetailResponseDto
import com.monitoring.dashboard.data.remote.dto.DashboardSearchHitDto
import com.monitoring.dashboard.data.remote.dto.DatasourceDto
import com.monitoring.dashboard.data.remote.dto.GrafanaHealthDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface for the Grafana HTTP API.
 *
 * All endpoints are relative to the base URL stored in EncryptedSharedPreferences.
 * Authentication is handled by [com.monitoring.dashboard.data.remote.interceptor.AuthInterceptor].
 */
interface GrafanaApiService {

    // ── Search / List Dashboards ──────────────────────────────────────────

    /**
     * Search dashboards & folders.
     *
     * @param query  Optional search query string.
     * @param type   Filter by type: "dash-db" (dashboards) or "dash-folder" (folders).
     * @param tag    Filter by tag.
     * @param starred Filter starred dashboards only.
     * @param limit  Maximum number of results (default 1000 on server).
     * @param page   Page number for pagination.
     * @param folderIds Folder IDs to search in.
     */
    @GET("api/search")
    suspend fun searchDashboards(
        @Query("query") query: String? = null,
        @Query("type") type: String? = null,
        @Query("tag") tag: String? = null,
        @Query("starred") starred: Boolean? = null,
        @Query("limit") limit: Int? = null,
        @Query("page") page: Int? = null,
        @Query("folderIds") folderIds: List<Long>? = null,
    ): Response<List<DashboardSearchHitDto>>

    // ── Dashboard Detail ──────────────────────────────────────────────────

    /**
     * Get a dashboard by its UID.
     *
     * @param uid The unique identifier of the dashboard.
     */
    @GET("api/dashboards/uid/{uid}")
    suspend fun getDashboardByUid(
        @Path("uid") uid: String,
    ): Response<DashboardDetailResponseDto>

    // ── Datasources ───────────────────────────────────────────────────────

    /**
     * Get all configured datasources.
     */
    @GET("api/datasources")
    suspend fun getDatasources(): Response<List<DatasourceDto>>

    /**
     * Get a single datasource by its numeric ID.
     *
     * @param id Datasource ID.
     */
    @GET("api/datasources/{id}")
    suspend fun getDatasourceById(
        @Path("id") id: Long,
    ): Response<DatasourceDto>

    /**
     * Get a single datasource by its UID.
     *
     * @param uid Datasource UID.
     */
    @GET("api/datasources/uid/{uid}")
    suspend fun getDatasourceByUid(
        @Path("uid") uid: String,
    ): Response<DatasourceDto>

    // ── Health ─────────────────────────────────────────────────────────────

    /**
     * Health check endpoint. Does NOT require authentication.
     */
    @GET("api/health")
    suspend fun getHealth(): Response<GrafanaHealthDto>
}

