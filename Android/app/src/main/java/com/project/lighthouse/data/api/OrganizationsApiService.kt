package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.CreateOrganizationRequest
import com.project.lighthouse.data.model.JoinOrganizationRequest
import com.project.lighthouse.data.model.OrganizationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface OrganizationsApiService {

    @GET("api/organizations")
    suspend fun getOrganizations(): Response<List<OrganizationResponse>>

    @POST("api/organizations")
    suspend fun createOrganization(
        @Body request: CreateOrganizationRequest
    ): Response<OrganizationResponse>

    @POST("api/organizations/join")
    suspend fun joinOrganization(
        @Body request: JoinOrganizationRequest
    ): Response<OrganizationResponse>
}

