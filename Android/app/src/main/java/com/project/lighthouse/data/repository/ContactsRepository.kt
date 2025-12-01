package com.project.lighthouse.data.repository

import android.util.Log
import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.ContactDto
import com.project.lighthouse.data.model.CreateContactRequest
import com.project.lighthouse.data.model.UpdateContactRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class ContactsRepository {

    suspend fun getContacts(): Result<List<ContactDto>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching contacts")
            val response = ApiClient.contactsApi.getContacts()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to fetch contacts" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun createContact(request: CreateContactRequest): Result<ContactDto> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating contact ${request.email}")
            val response = ApiClient.contactsApi.createContact(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to create contact" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun updateContact(contactId: String, request: UpdateContactRequest): Result<ContactDto> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating contact $contactId")
            val response = ApiClient.contactsApi.updateContact(contactId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to update contact" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun deleteContact(contactId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.contactsApi.deleteContact(contactId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to delete contact" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    companion object {
        private const val TAG = "ContactsRepository"
    }
}

