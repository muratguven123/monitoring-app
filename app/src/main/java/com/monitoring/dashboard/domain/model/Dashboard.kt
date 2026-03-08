package com.monitoring.dashboard.domain.model

import com.monitoring.dashboard.data.remote.dto.DashboardSearchHitDto

/**
 * Clean domain model for a dashboard list item.
 * Mapped from [DashboardSearchHitDto].
 */
data class Dashboard(
    val id: Long,
    val uid: String,
    val title: String,
    val url: String,
    val type: String,
    val tags: List<String>,
    val isStarred: Boolean,
    val folderTitle: String?,
)

fun DashboardSearchHitDto.toDomain() = Dashboard(
    id = id,
    uid = uid,
    title = title,
    url = url,
    type = type,
    tags = tags,
    isStarred = isStarred,
    folderTitle = folderTitle,
)
