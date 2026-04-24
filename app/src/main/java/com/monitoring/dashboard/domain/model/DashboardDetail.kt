package com.monitoring.dashboard.domain.model

import com.monitoring.dashboard.data.remote.dto.DashboardDetailResponseDto
import com.monitoring.dashboard.data.remote.dto.PanelDto

/**
 * Clean domain model for a full dashboard with its panels.
 */
data class DashboardDetail(
    val uid: String,
    val title: String,
    val tags: List<String>,
    val refresh: String?,
    val timezone: String?,
    val panels: List<Panel>,
    val folderTitle: String?,
    val updatedBy: String?,
    val updated: String?,
)

data class Panel(
    val id: Long,
    val title: String,
    val type: String,
    val description: String?,
    val datasourceUid: String?,
    val width: Int,
    val height: Int,
)

fun DashboardDetailResponseDto.toDomain() = DashboardDetail(
    uid = dashboard.uid,
    title = dashboard.title,
    tags = dashboard.tags,
    refresh = dashboard.refresh,
    timezone = dashboard.timezone,
    panels = dashboard.panels
        .filter { it.type != "row" }          // Satır ayraçlarını filtrele
        .map { it.toDomain() },
    folderTitle = meta.folderTitle,
    updatedBy = meta.updatedBy,
    updated = meta.updated,
)

fun PanelDto.toDomain() = Panel(
    id = id,
    title = title,
    type = type,
    description = description,
    datasourceUid = datasource?.uid,
    width = gridPos?.w ?: 12,
    height = gridPos?.h ?: 8,
)
