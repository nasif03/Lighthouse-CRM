package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.SwitchTenantRequest
import com.project.lighthouse.data.model.TenantListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TenantsApiService {

    @GET("api/tenants")
    suspend fun getTenants(): Response<TenantListResponse>

    @POST("api/tenants/switch")
    suspend fun switchTenant(
        @Body request: SwitchTenantRequest
    ): Response<Map<String, String>>
}

