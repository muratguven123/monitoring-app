package com.monitoring.dashboard.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Grafana `/api/datasources` endpoint response item.
 * Represents a configured datasource in Grafana.
 */
data class DatasourceDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("uid")
    val uid: String,

    @SerializedName("orgId")
    val orgId: Long? = null,

    @SerializedName("name")
    val name: String,

    @SerializedName("type")
    val type: String, // e.g. "prometheus", "influxdb", "elasticsearch", "loki"

    @SerializedName("typeName")
    val typeName: String? = null,

    @SerializedName("typeLogoUrl")
    val typeLogoUrl: String? = null,

    @SerializedName("access")
    val access: String? = null, // "proxy" or "direct"

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("user")
    val user: String? = null,

    @SerializedName("database")
    val database: String? = null,

    @SerializedName("basicAuth")
    val basicAuth: Boolean = false,

    @SerializedName("isDefault")
    val isDefault: Boolean = false,

    @SerializedName("readOnly")
    val readOnly: Boolean = false,
)

