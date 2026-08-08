package com.monitoring.dashboard.domain.usecase

import com.monitoring.dashboard.data.local.dao.AlertDao
import com.monitoring.dashboard.domain.model.AlertViolation
import com.monitoring.dashboard.domain.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetOpenViolationsUseCase @Inject constructor(
    private val alertDao: AlertDao,
) {
    fun observe(): Flow<List<AlertViolation>> =
        alertDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getOpen(): List<AlertViolation> =
        alertDao.getOpen().map { it.toDomain() }
}
