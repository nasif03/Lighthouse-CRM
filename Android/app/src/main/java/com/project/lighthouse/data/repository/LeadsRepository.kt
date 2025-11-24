package com.project.lighthouse.data.repository

import android.util.Log
import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.ConvertLeadResponse
import com.project.lighthouse.data.model.CreateLeadRequest
import com.project.lighthouse.data.model.LeadDto
import com.project.lighthouse.data.model.UpdateLeadStatusRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class LeadsRepository {

    suspend fun getLeads(skip: Int = 0, limit: Int = 100): Result<List<LeadDto>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching leads skip=$skip limit=$limit")
                val response = ApiClient.leadsApi.getLeads(skip, limit)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    Log.e(TAG, "Leads fetch error ${response.code()} $errorBody")
                    Result.failure(
                        ApiException.HttpError(
                            code = response.code(),
                            message = errorBody.ifBlank { "Failed to fetch leads" }
                        )
                    )
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error fetching leads: ${e.message}", e)
                Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
            } catch (e: Exception) {
                Log.e(TAG, "Unknown error fetching leads: ${e.message}", e)
                Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
            }
        }

    suspend fun createLead(request: CreateLeadRequest): Result<LeadDto> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Creating lead ${request.name}")
                val response = ApiClient.leadsApi.createLead(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    Log.e(TAG, "Create lead error ${response.code()} $errorBody")
                    Result.failure(
                        ApiException.HttpError(
                            code = response.code(),
                            message = errorBody.ifBlank { "Failed to create lead" }
                        )
                    )
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error creating lead: ${e.message}", e)
                Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
            } catch (e: Exception) {
                Log.e(TAG, "Unknown error creating lead: ${e.message}", e)
                Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
            }
        }

    suspend fun updateLeadStatus(leadId: String, status: String): Result<LeadDto> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating lead $leadId status -> $status")
                val response = ApiClient.leadsApi.updateLeadStatus(
                    leadId,
                    UpdateLeadStatusRequest(status)
                )
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    Log.e(TAG, "Update status error ${response.code()} $errorBody")
                    Result.failure(
                        ApiException.HttpError(
                            code = response.code(),
                            message = errorBody.ifBlank { "Failed to update lead status" }
                        )
                    )
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error updating lead status: ${e.message}", e)
                Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
            } catch (e: Exception) {
                Log.e(TAG, "Unknown error updating lead status: ${e.message}", e)
                Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
            }
        }

    suspend fun convertLead(leadId: String): Result<ConvertLeadResponse> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Converting lead $leadId")
                val response = ApiClient.leadsApi.convertLead(leadId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    Log.e(TAG, "Convert lead error ${response.code()} $errorBody")
                    Result.failure(
                        ApiException.HttpError(
                            code = response.code(),
                            message = errorBody.ifBlank { "Failed to convert lead" }
                        )
                    )
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error converting lead: ${e.message}", e)
                Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
            } catch (e: Exception) {
                Log.e(TAG, "Unknown error converting lead: ${e.message}", e)
                Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
            }
        }

    suspend fun deleteLead(leadId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting lead $leadId")
                val response = ApiClient.leadsApi.deleteLead(leadId)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    Log.e(TAG, "Delete lead error ${response.code()} $errorBody")
                    Result.failure(
                        ApiException.HttpError(
                            code = response.code(),
                            message = errorBody.ifBlank { "Failed to delete lead" }
                        )
                    )
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error deleting lead: ${e.message}", e)
                Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
            } catch (e: Exception) {
                Log.e(TAG, "Unknown error deleting lead: ${e.message}", e)
                Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
            }
        }

    companion object {
        private const val TAG = "LeadsRepository"
    }
}

