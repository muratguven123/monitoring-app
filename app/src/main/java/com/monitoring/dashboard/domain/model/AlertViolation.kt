package com.monitoring.dashboard.domain.model

import com.monitoring.dashboard.data.local.entity.AlertViolationEntity
import com.monitoring.dashboard.data.remote.dto.newrelic.AlertViolationDto

data class AlertViolation(
    val id: Long,
    val label: String?,
    val policyName: String?,
    val conditionName: String?,
    val severity: String?,
    val openedAt: Long?,
    val isOpen: Boolean,
    val resolvedAt: Long?,
)

fun AlertViolationDto.toDomain(isOpen: Boolean = closedAt == null): AlertViolation =
    AlertViolation(
        id = id,
        label = label,
        policyName = policyName,
        conditionName = conditionName,
        severity = priority,
        openedAt = openedAt,
        isOpen = isOpen,
        resolvedAt = closedAt,
    )

fun AlertViolationEntity.toDomain(): AlertViolation =
    AlertViolation(
        id = id,
        label = label,
        policyName = policyName,
        conditionName = conditionName,
        severity = severity,
        openedAt = openedAt,
        isOpen = isOpen,
        resolvedAt = resolvedAt,
    )

fun AlertViolation.toEntity(): AlertViolationEntity =
    AlertViolationEntity(
        id = id,
        label = label,
        policyName = policyName,
        conditionName = conditionName,
        openedAt = openedAt,
        severity = severity,
        isOpen = isOpen,
        resolvedAt = resolvedAt,
    )
