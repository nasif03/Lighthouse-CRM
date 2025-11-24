package com.project.lighthouse.data.repository

import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.CreateOrganizationRequest
import com.project.lighthouse.data.model.JoinOrganizationRequest
import com.project.lighthouse.data.model.OrganizationResponse
import com.project.lighthouse.data.model.SwitchTenantRequest
import com.project.lighthouse.data.model.TenantListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class OrganizationRepository {

    suspend fun getOrganizations(): Result<List<OrganizationResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.organizationsApi.getOrganizations()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to load organizations" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun createOrganization(request: CreateOrganizationRequest): Result<OrganizationResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.organizationsApi.createOrganization(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to create organization" }))
                }
            } catch (e: IOException) {
                Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
            } catch (e: Exception) {
                Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
            }
        }

    suspend fun joinOrganization(request: JoinOrganizationRequest): Result<OrganizationResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.organizationsApi.joinOrganization(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to join organization" }))
                }
            } catch (e: IOException) {
                Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
            } catch (e: Exception) {
                Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
            }
        }

    suspend fun getTenants(): Result<TenantListResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.tenantsApi.getTenants()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to load tenants" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun switchTenant(tenantId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.tenantsApi.switchTenant(SwitchTenantRequest(tenantId))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to switch tenant" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }
}

