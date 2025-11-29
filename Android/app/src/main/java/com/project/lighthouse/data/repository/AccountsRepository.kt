package com.project.lighthouse.data.repository

import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.AccountDto
import com.project.lighthouse.data.model.AccountDetailsResponse
import com.project.lighthouse.data.model.CreateAccountRequest
import com.project.lighthouse.data.model.UpdateAccountRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AccountsRepository {

    suspend fun getAccounts(): Result<List<AccountDto>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.accountsApi.getAccounts()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to fetch accounts" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun createAccount(request: CreateAccountRequest): Result<AccountDto> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.accountsApi.createAccount(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to create account" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun updateAccount(accountId: String, request: UpdateAccountRequest): Result<AccountDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.accountsApi.updateAccount(accountId, request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to update account" }))
                }
            } catch (e: IOException) {
                Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
            } catch (e: Exception) {
                Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
            }
        }

    suspend fun deleteAccount(accountId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.accountsApi.deleteAccount(accountId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to delete account" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun getAccountDetails(accountId: String): Result<AccountDetailsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.accountsApi.getAccountDetails(accountId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to fetch account details" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }
}

