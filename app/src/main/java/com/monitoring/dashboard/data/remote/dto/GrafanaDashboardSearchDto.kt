package com.monitoring.dashboard.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Grafana `/api/search` endpoint response item.
 * Represents a single dashboard/folder hit returned by the search API.
 */
data class DashboardSearchHitDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("uid")
    val uid: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("uri")
    val uri: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("slug")
    val slug: String,

    @SerializedName("type")
    val type: String, // "dash-db" or "dash-folder"

    @SerializedName("tags")
    val tags: List<String>,

    @SerializedName("isStarred")
    val isStarred: Boolean,

    @SerializedName("folderId")
    val folderId: Long? = null,

    @SerializedName("folderUid")
    val folderUid: String? = null,

    @SerializedName("folderTitle")
    val folderTitle: String? = null,

    @SerializedName("folderUrl")
    val folderUrl: String? = null,

    @SerializedName("sortMeta")
    val sortMeta: Long? = null,
)

