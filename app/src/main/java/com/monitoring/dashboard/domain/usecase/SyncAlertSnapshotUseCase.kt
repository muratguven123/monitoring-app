package com.monitoring.dashboard.domain.usecase

import com.monitoring.dashboard.data.local.dao.AlertDao
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.data.repository.NewRelicRepository
import com.monitoring.dashboard.domain.model.AlertViolation
import com.monitoring.dashboard.domain.model.toDomain
import com.monitoring.dashboard.domain.model.toEntity
import javax.inject.Inject

data class AlertSyncResult(
    val openViolations: List<AlertViolation>,
    val newlyOpened: List<AlertViolation>,
    val newlyResolvedIds: List<Long>,
)

/**
 * Upserts the current open-violation snapshot and marks missing ones as resolved.
 */
class SyncAlertSnapshotUseCase @Inject constructor(
    private val newRelicRepository: NewRelicRepository,
    private val alertDao: AlertDao,
) {
    suspend operator fun invoke(): NetworkResult<AlertSyncResult> {
        return when (val result = newRelicRepository.getAlertViolations(onlyOpen = true)) {
            is NetworkResult.Success -> {
                val open = result.data.map { it.toDomain(isOpen = true) }
                val previousOpen = alertDao.getOpen()
                val previousIds = previousOpen.map { it.id }.toSet()
                val currentIds = open.map { it.id }.toSet()

                val newlyOpened = open.filter { it.id !in previousIds }
                val newlyResolvedIds = previousIds.filter { it !in currentIds }

                if (newlyResolvedIds.isNotEmpty()) {
                    alertDao.markResolved(newlyResolvedIds, System.currentTimeMillis())
                }
                if (open.isNotEmpty()) {
                    alertDao.insertAll(open.map { it.toEntity() })
                }

                NetworkResult.Success(
                    AlertSyncResult(
                        openViolations = open,
                        newlyOpened = newlyOpened,
                        newlyResolvedIds = newlyResolvedIds,
                    ),
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.exception)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }
}
