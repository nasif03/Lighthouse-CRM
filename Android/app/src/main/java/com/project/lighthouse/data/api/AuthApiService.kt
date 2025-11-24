package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.OrganizationResponse
import com.project.lighthouse.data.model.TokenResponse
import com.project.lighthouse.data.model.UserResponse
import com.project.lighthouse.data.model.VerifyTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/auth/verify-token")
    suspend fun verifyToken(@Body request: VerifyTokenRequest): Response<TokenResponse>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Map<String, String>>

    @GET("api/auth/organizations")
    suspend fun getOrganizations(): Response<List<OrganizationResponse>>
}

