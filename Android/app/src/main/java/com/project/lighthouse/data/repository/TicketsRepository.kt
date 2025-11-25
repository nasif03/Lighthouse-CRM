package com.project.lighthouse.data.repository

import android.util.Log
import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.AdminCheckResponse
import com.project.lighthouse.data.model.AssignableEmployee
import com.project.lighthouse.data.model.CreateTicketRequest
import com.project.lighthouse.data.model.TicketDto
import com.project.lighthouse.data.model.UpdateTicketRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class TicketsRepository {
    private val api = ApiClient.ticketsApi

    suspend fun getTickets(
        skip: Int = 0,
        limit: Int = 100,
        status: String? = null,
        priority: String? = null,
        assignedTo: String? = null
    ): Result<List<TicketDto>> = withContext(Dispatchers.IO) {
        try {
            Log.d("TicketsRepository", "Getting tickets: skip=$skip, limit=$limit, status=$status, priority=$priority")
            val response = api.getTickets(skip, limit, status, priority, assignedTo)
            
            if (response.isSuccessful && response.body() != null) {
                val tickets = response.body()!!
                Log.d("TicketsRepository", "Retrieved ${tickets.size} tickets")
                Result.success(tickets)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("TicketsRepository", "Failed to get tickets: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("TicketsRepository", "Network error getting tickets", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("TicketsRepository", "Unexpected error getting tickets", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun getTicket(ticketId: String): Result<TicketDto> = withContext(Dispatchers.IO) {
        try {
            Log.d("TicketsRepository", "Getting ticket: $ticketId")
            val response = api.getTicket(ticketId)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d("TicketsRepository", "Ticket retrieved successfully")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("TicketsRepository", "Failed to get ticket: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("TicketsRepository", "Network error getting ticket", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("TicketsRepository", "Unexpected error getting ticket", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun updateTicket(
        ticketId: String,
        status: String? = null,
        priority: String? = null,
        assignedTo: String? = null,
        category: String? = null
    ): Result<TicketDto> = withContext(Dispatchers.IO) {
        try {
            Log.d("TicketsRepository", "Updating ticket: $ticketId, status=$status, priority=$priority")
            val request = UpdateTicketRequest(status, priority, assignedTo, category)
            val response = api.updateTicket(ticketId, request)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d("TicketsRepository", "Ticket updated successfully")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("TicketsRepository", "Failed to update ticket: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("TicketsRepository", "Network error updating ticket", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("TicketsRepository", "Unexpected error updating ticket", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun createTicket(
        orgId: String,
        name: String,
        email: String,
        subject: String,
        description: String,
        phone: String? = null,
        priority: String? = "medium",
        category: String? = null
    ): Result<TicketDto> = withContext(Dispatchers.IO) {
        try {
            Log.d("TicketsRepository", "Creating ticket: subject=$subject")
            val request = CreateTicketRequest(orgId, name, email, phone, subject, description, priority, category)
            val response = api.createTicket(request)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d("TicketsRepository", "Ticket created successfully: ${response.body()?.ticketNumber}")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("TicketsRepository", "Failed to create ticket: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("TicketsRepository", "Network error creating ticket", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("TicketsRepository", "Unexpected error creating ticket", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun checkAdmin(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d("TicketsRepository", "Checking admin status")
            val response = api.checkAdmin()
            
            if (response.isSuccessful && response.body() != null) {
                val isAdmin = response.body()!!.isAdmin
                Log.d("TicketsRepository", "Admin status: $isAdmin")
                Result.success(isAdmin)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("TicketsRepository", "Failed to check admin: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("TicketsRepository", "Network error checking admin", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("TicketsRepository", "Unexpected error checking admin", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun getAssignableEmployees(): Result<List<AssignableEmployee>> = withContext(Dispatchers.IO) {
        try {
            Log.d("TicketsRepository", "Getting assignable employees")
            val response = api.getAssignableEmployees()
            
            if (response.isSuccessful && response.body() != null) {
                val employees = response.body()!!
                Log.d("TicketsRepository", "Retrieved ${employees.size} assignable employees")
                Result.success(employees)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("TicketsRepository", "Failed to get assignable employees: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("TicketsRepository", "Network error getting assignable employees", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("TicketsRepository", "Unexpected error getting assignable employees", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }
}

