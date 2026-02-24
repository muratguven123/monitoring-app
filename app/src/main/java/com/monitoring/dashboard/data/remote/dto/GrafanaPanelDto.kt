package com.monitoring.dashboard.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Represents a single panel inside a Grafana dashboard.
 * Mapped from the `panels` array in `/api/dashboards/uid/{uid}` response.
 */
data class PanelDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("type")
    val type: String, // e.g. "graph", "stat", "gauge", "table", "timeseries", "row"

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("datasource")
    val datasource: PanelDatasourceDto? = null,

    @SerializedName("gridPos")
    val gridPos: GridPosDto? = null,

    @SerializedName("targets")
    val targets: List<PanelTargetDto> = emptyList(),

    @SerializedName("transparent")
    val transparent: Boolean = false,

    @SerializedName("panels")
    val panels: List<PanelDto>? = null, // nested panels inside "row" type
)

/**
 * Datasource reference within a panel. Can be a string or an object in Grafana JSON,
 * but we model the object form here.
 */
data class PanelDatasourceDto(
    @SerializedName("type")
    val type: String? = null,

    @SerializedName("uid")
    val uid: String? = null,
)

/**
 * Grid position of a panel on the dashboard canvas.
 */
data class GridPosDto(
    @SerializedName("h")
    val h: Int,

    @SerializedName("w")
    val w: Int,

    @SerializedName("x")
    val x: Int,

    @SerializedName("y")
    val y: Int,
)

/**
 * A query target within a panel.
 */
data class PanelTargetDto(
    @SerializedName("refId")
    val refId: String? = null,

    @SerializedName("expr")
    val expr: String? = null, // Prometheus expression

    @SerializedName("legendFormat")
    val legendFormat: String? = null,

    @SerializedName("datasource")
    val datasource: PanelDatasourceDto? = null,
)

