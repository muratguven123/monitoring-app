package com.monitoring.dashboard.data.remote.dto.newrelic

import com.google.gson.annotations.SerializedName

/**
 * New Relic `/v2/applications/{id}/metrics.json` response.
 */
data class NewRelicMetricNamesResponseDto(
    @SerializedName("metrics")
    val metrics: List<MetricNameDto>,
)

data class MetricNameDto(
    @SerializedName("name")
    val name: String,

    @SerializedName("values")
    val values: List<String> = emptyList(),
)

/**
 * New Relic `/v2/applications/{id}/metrics/data.json` response.
 */
data class NewRelicMetricDataResponseDto(
    @SerializedName("metric_data")
    val metricData: MetricDataDto,
)

data class MetricDataDto(
    @SerializedName("from")
    val from: String? = null,

    @SerializedName("to")
    val to: String? = null,

    @SerializedName("metrics_not_found")
    val metricsNotFound: List<String> = emptyList(),

    @SerializedName("metrics_found")
    val metricsFound: List<String> = emptyList(),

    @SerializedName("metrics")
    val metrics: List<MetricTimeSliceDto> = emptyList(),
)

data class MetricTimeSliceDto(
    @SerializedName("name")
    val name: String,

    @SerializedName("timeslices")
    val timeslices: List<TimeSliceDto> = emptyList(),
)

data class TimeSliceDto(
    @SerializedName("from")
    val from: String,

    @SerializedName("to")
    val to: String,

    @SerializedName("values")
    val values: Map<String, Double> = emptyMap(),
)
