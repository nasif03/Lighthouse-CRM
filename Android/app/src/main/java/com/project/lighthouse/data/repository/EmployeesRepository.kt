package com.project.lighthouse.data.repository

import android.util.Log
import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.CreateEmployeeRequest
import com.project.lighthouse.data.model.EmployeeDto
import com.project.lighthouse.data.model.UpdateEmployeeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class EmployeesRepository {
    private val api = ApiClient.employeesApi

    suspend fun getEmployees(orgId: String): Result<List<EmployeeDto>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getEmployees(orgId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Log.e("EmployeesRepository", "Failed to get employees: ${response.code()} - $errorBody")
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to fetch employees" }))
            }
        } catch (e: IOException) {
            Log.e("EmployeesRepository", "Network error getting employees", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("EmployeesRepository", "Unexpected error getting employees", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun createEmployee(orgId: String, name: String, email: String, roleIds: List<String>): Result<EmployeeDto> = withContext(Dispatchers.IO) {
        try {
            val request = CreateEmployeeRequest(name, email, roleIds)
            val response = api.createEmployee(orgId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Log.e("EmployeesRepository", "Failed to create employee: ${response.code()} - $errorBody")
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to create employee" }))
            }
        } catch (e: IOException) {
            Log.e("EmployeesRepository", "Network error creating employee", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("EmployeesRepository", "Unexpected error creating employee", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun updateEmployee(orgId: String, employeeId: String, roleIds: List<String>): Result<EmployeeDto> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateEmployeeRequest(roleIds)
            val response = api.updateEmployee(orgId, employeeId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Log.e("EmployeesRepository", "Failed to update employee: ${response.code()} - $errorBody")
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to update employee" }))
            }
        } catch (e: IOException) {
            Log.e("EmployeesRepository", "Network error updating employee", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("EmployeesRepository", "Unexpected error updating employee", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }
}

