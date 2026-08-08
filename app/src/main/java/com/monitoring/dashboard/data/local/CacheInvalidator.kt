package com.monitoring.dashboard.data.local

import com.monitoring.dashboard.data.local.dao.AlertDao
import com.monitoring.dashboard.data.local.dao.GrafanaDao
import com.monitoring.dashboard.data.local.dao.NewRelicDao
import com.monitoring.dashboard.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clears all Room caches when the active environment profile changes
 * so Staging/Prod data cannot leak across profiles.
 */
@Singleton
class CacheInvalidator @Inject constructor(
    private val grafanaDao: GrafanaDao,
    private val newRelicDao: NewRelicDao,
    private val alertDao: AlertDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun clearAll() = withContext(ioDispatcher) {
        grafanaDao.deleteAll()
        newRelicDao.deleteAll()
        alertDao.deleteAll()
        Timber.i("CacheInvalidator: all Room caches cleared")
    }
}
