package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.CreateJiraIssueRequest
import com.project.lighthouse.data.model.CreateJiraIssueResponse
import com.project.lighthouse.data.model.JiraIssue
import com.project.lighthouse.data.model.JiraProjectResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JiraApiService {
    @POST("api/jira/projects/{org_id}")
    suspend fun createProject(@Path("org_id") orgId: String): Response<JiraProjectResponse>

    @POST("api/jira/software/projects/{org_id}")
    suspend fun createSoftwareProject(@Path("org_id") orgId: String): Response<JiraProjectResponse>

    @POST("api/jira/tickets/{ticket_id}/create-issue")
    suspend fun createIssueForTicket(
        @Path("ticket_id") ticketId: String,
        @Body request: CreateJiraIssueRequest
    ): Response<CreateJiraIssueResponse>

    @GET("api/jira/issues")
    suspend fun getIssues(@Query("project_type") projectType: String = "jsm"): Response<List<JiraIssue>>

    @GET("api/jira/issues/{issue_key}")
    suspend fun getIssue(@Path("issue_key") issueKey: String): Response<JiraIssue>
}

