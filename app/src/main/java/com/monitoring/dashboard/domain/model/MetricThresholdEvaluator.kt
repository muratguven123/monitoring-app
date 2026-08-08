package com.monitoring.dashboard.domain.model

import com.monitoring.dashboard.data.local.MetricThresholds

enum class MetricHealth { HEALTHY, WARNING, CRITICAL }

/**
 * Domain rules for color-coding New Relic metrics from configurable thresholds.
 */
object MetricThresholdEvaluator {

    fun apdex(value: Double, thresholds: MetricThresholds): MetricHealth = when {
        value >= thresholds.apdexGreen -> MetricHealth.HEALTHY
        value >= thresholds.apdexYellow -> MetricHealth.WARNING
        else -> MetricHealth.CRITICAL
    }

    /** Lower is better. */
    fun responseTimeMs(value: Double, thresholds: MetricThresholds): MetricHealth = when {
        value < thresholds.responseTimeGreenMs -> MetricHealth.HEALTHY
        value < thresholds.responseTimeYellowMs -> MetricHealth.WARNING
        else -> MetricHealth.CRITICAL
    }

    fun errorRatePercent(value: Double, thresholds: MetricThresholds): MetricHealth = when {
        value < thresholds.errorRateGreen -> MetricHealth.HEALTHY
        value < thresholds.errorRateYellow -> MetricHealth.WARNING
        else -> MetricHealth.CRITICAL
    }

    fun pageLoadMs(value: Double, thresholds: MetricThresholds): MetricHealth = when {
        value < thresholds.pageLoadGreenMs -> MetricHealth.HEALTHY
        value < thresholds.pageLoadYellowMs -> MetricHealth.WARNING
        else -> MetricHealth.CRITICAL
    }
}
