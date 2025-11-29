package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.CreateRoleRequest
import com.project.lighthouse.data.model.RoleDto
import com.project.lighthouse.data.model.UpdateRoleRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RolesApiService {
    @GET("api/organizations/{org_id}/roles")
    suspend fun getRoles(@Path("org_id") orgId: String): Response<List<RoleDto>>

    @POST("api/organizations/{org_id}/roles")
    suspend fun createRole(
        @Path("org_id") orgId: String,
        @Body request: CreateRoleRequest
    ): Response<RoleDto>

    @PUT("api/organizations/{org_id}/roles/{role_id}")
    suspend fun updateRole(
        @Path("org_id") orgId: String,
        @Path("role_id") roleId: String,
        @Body request: UpdateRoleRequest
    ): Response<RoleDto>

    @DELETE("api/organizations/{org_id}/roles/{role_id}")
    suspend fun deleteRole(
        @Path("org_id") orgId: String,
        @Path("role_id") roleId: String
    ): Response<Map<String, String>>
}

