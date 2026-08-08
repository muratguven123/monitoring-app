package com.monitoring.dashboard.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class GitHubWorkflowRunDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("conclusion") val conclusion: String?,
    @SerializedName("html_url") val htmlUrl: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("head_branch") val headBranch: String?,
)

data class GitHubWorkflowRunsResponse(
    @SerializedName("workflow_runs") val workflowRuns: List<GitHubWorkflowRunDto> = emptyList(),
)

interface GitHubApiService {
    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getWorkflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 10,
    ): Response<GitHubWorkflowRunsResponse>
}
