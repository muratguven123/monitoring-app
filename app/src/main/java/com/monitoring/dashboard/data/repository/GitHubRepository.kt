package com.monitoring.dashboard.data.repository

import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.remote.GitHubApiService
import com.monitoring.dashboard.data.remote.GitHubWorkflowRunDto
import com.monitoring.dashboard.data.remote.util.NetworkResult
import com.monitoring.dashboard.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubRepository @Inject constructor(
    private val apiService: GitHubApiService,
    private val securePreferencesManager: SecurePreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun getRecentWorkflowRuns(): NetworkResult<List<GitHubWorkflowRunDto>> =
        withContext(ioDispatcher) {
            val repo = securePreferencesManager.getGithubRepo()
            if (repo.isNullOrBlank() || !repo.contains("/")) {
                return@withContext NetworkResult.Error(message = "Configure GitHub repo as owner/name")
            }
            val (owner, name) = repo.split("/", limit = 2)
            try {
                val response = apiService.getWorkflowRuns(owner.trim(), name.trim())
                if (response.isSuccessful) {
                    NetworkResult.Success(response.body()?.workflowRuns.orEmpty())
                } else {
                    NetworkResult.Error(code = response.code(), message = response.message())
                }
            } catch (e: Exception) {
                NetworkResult.Error(message = e.message, exception = e)
            }
        }
}
