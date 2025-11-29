package com.project.lighthouse.data.repository

import android.util.Log
import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.CreateRoleRequest
import com.project.lighthouse.data.model.RoleDto
import com.project.lighthouse.data.model.UpdateRoleRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class RolesRepository {
    private val api = ApiClient.rolesApi

    suspend fun getRoles(orgId: String): Result<List<RoleDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getRoles(orgId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Log.e("RolesRepository", "Failed to get roles: ${response.code()} - $errorBody")
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to fetch roles" }))
            }
        } catch (e: IOException) {
            Log.e("RolesRepository", "Network error getting roles", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("RolesRepository", "Unexpected error getting roles", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun createRole(orgId: String, name: String, permissions: List<String>): Result<RoleDto> = withContext(Dispatchers.IO) {
        try {
            val request = CreateRoleRequest(name, permissions)
            val response = api.createRole(orgId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Log.e("RolesRepository", "Failed to create role: ${response.code()} - $errorBody")
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to create role" }))
            }
        } catch (e: IOException) {
            Log.e("RolesRepository", "Network error creating role", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("RolesRepository", "Unexpected error creating role", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun updateRole(orgId: String, roleId: String, name: String?, permissions: List<String>?): Result<RoleDto> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateRoleRequest(name, permissions)
            val response = api.updateRole(orgId, roleId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Log.e("RolesRepository", "Failed to update role: ${response.code()} - $errorBody")
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to update role" }))
            }
        } catch (e: IOException) {
            Log.e("RolesRepository", "Network error updating role", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("RolesRepository", "Unexpected error updating role", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun deleteRole(orgId: String, roleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteRole(orgId, roleId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Log.e("RolesRepository", "Failed to delete role: ${response.code()} - $errorBody")
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to delete role" }))
            }
        } catch (e: IOException) {
            Log.e("RolesRepository", "Network error deleting role", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("RolesRepository", "Unexpected error deleting role", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }
}

