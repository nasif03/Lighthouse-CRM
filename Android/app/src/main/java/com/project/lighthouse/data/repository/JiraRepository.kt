package com.project.lighthouse.data.repository

import android.util.Log
import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.CreateJiraIssueRequest
import com.project.lighthouse.data.model.CreateJiraIssueResponse
import com.project.lighthouse.data.model.JiraIssue
import com.project.lighthouse.data.model.JiraProjectResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class JiraRepository {
    private val api = ApiClient.jiraApi

    suspend fun createProject(orgId: String): Result<JiraProjectResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("JiraRepository", "Creating Jira project for org: $orgId")
            val response = api.createProject(orgId)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d("JiraRepository", "Jira project created: ${response.body()?.projectKey}")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("JiraRepository", "Failed to create project: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("JiraRepository", "Network error creating project", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("JiraRepository", "Unexpected error creating project", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun createSoftwareProject(orgId: String): Result<JiraProjectResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("JiraRepository", "Creating Jira Software project for org: $orgId")
            val response = api.createSoftwareProject(orgId)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d("JiraRepository", "Jira Software project created: ${response.body()?.projectKey}")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("JiraRepository", "Failed to create software project: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("JiraRepository", "Network error creating software project", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("JiraRepository", "Unexpected error creating software project", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun createIssueForTicket(ticketId: String): Result<CreateJiraIssueResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("JiraRepository", "Creating Jira issue for ticket: $ticketId")
            val request = CreateJiraIssueRequest(ticketId = ticketId)
            val response = api.createIssueForTicket(ticketId, request)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d("JiraRepository", "Jira issue created: ${response.body()?.issueKey}")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("JiraRepository", "Failed to create issue: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("JiraRepository", "Network error creating issue", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("JiraRepository", "Unexpected error creating issue", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun getIssues(projectType: String = "jsm"): Result<List<JiraIssue>> = withContext(Dispatchers.IO) {
        try {
            Log.d("JiraRepository", "Getting Jira issues: projectType=$projectType")
            val response = api.getIssues(projectType)
            
            if (response.isSuccessful && response.body() != null) {
                val issues = response.body()!!
                Log.d("JiraRepository", "Retrieved ${issues.size} issues")
                Result.success(issues)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("JiraRepository", "Failed to get issues: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("JiraRepository", "Network error getting issues", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("JiraRepository", "Unexpected error getting issues", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun getIssue(issueKey: String): Result<JiraIssue> = withContext(Dispatchers.IO) {
        try {
            Log.d("JiraRepository", "Getting Jira issue: $issueKey")
            val response = api.getIssue(issueKey)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d("JiraRepository", "Issue retrieved successfully")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("JiraRepository", "Failed to get issue: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("JiraRepository", "Network error getting issue", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("JiraRepository", "Unexpected error getting issue", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }
}

