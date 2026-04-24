package com.monitoring.dashboard.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Grafana `/api/dashboards/uid/{uid}` endpoint response.
 */
data class DashboardDetailResponseDto(
    @SerializedName("meta")
    val meta: DashboardMetaDto,

    @SerializedName("dashboard")
    val dashboard: DashboardDto,
)

data class DashboardMetaDto(
    @SerializedName("type")
    val type: String? = null,

    @SerializedName("canStar")
    val canStar: Boolean = false,

    @SerializedName("canSave")
    val canSave: Boolean = false,

    @SerializedName("canEdit")
    val canEdit: Boolean = false,

    @SerializedName("canAdmin")
    val canAdmin: Boolean = false,

    @SerializedName("canDelete")
    val canDelete: Boolean = false,

    @SerializedName("slug")
    val slug: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("expires")
    val expires: String? = null,

    @SerializedName("created")
    val created: String? = null,

    @SerializedName("updated")
    val updated: String? = null,

    @SerializedName("updatedBy")
    val updatedBy: String? = null,

    @SerializedName("createdBy")
    val createdBy: String? = null,

    @SerializedName("version")
    val version: Int = 0,

    @SerializedName("folderId")
    val folderId: Long? = null,

    @SerializedName("folderUid")
    val folderUid: String? = null,

    @SerializedName("folderTitle")
    val folderTitle: String? = null,

    @SerializedName("folderUrl")
    val folderUrl: String? = null,

    @SerializedName("isStarred")
    val isStarred: Boolean = false,
)

data class DashboardDto(
    @SerializedName("id")
    val id: Long?,

    @SerializedName("uid")
    val uid: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("tags")
    val tags: List<String> = emptyList(),

    @SerializedName("timezone")
    val timezone: String? = null,

    @SerializedName("schemaVersion")
    val schemaVersion: Int? = null,

    @SerializedName("version")
    val version: Int? = null,

    @SerializedName("refresh")
    val refresh: String? = null,

    @SerializedName("panels")
    val panels: List<PanelDto> = emptyList(),
)

