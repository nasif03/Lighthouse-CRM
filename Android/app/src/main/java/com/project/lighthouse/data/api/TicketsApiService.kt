package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.AdminCheckResponse
import com.project.lighthouse.data.model.AssignableEmployee
import com.project.lighthouse.data.model.CreateTicketRequest
import com.project.lighthouse.data.model.TicketDto
import com.project.lighthouse.data.model.UpdateTicketRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TicketsApiService {
    @GET("api/tickets")
    suspend fun getTickets(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100,
        @Query("status") status: String? = null,
        @Query("priority") priority: String? = null,
        @Query("assignedTo") assignedTo: String? = null
    ): Response<List<TicketDto>>

    @GET("api/tickets/{ticket_id}")
    suspend fun getTicket(@Path("ticket_id") ticketId: String): Response<TicketDto>

    @PUT("api/tickets/{ticket_id}")
    suspend fun updateTicket(
        @Path("ticket_id") ticketId: String,
        @Body request: UpdateTicketRequest
    ): Response<TicketDto>

    @POST("api/tickets")
    suspend fun createTicket(@Body request: CreateTicketRequest): Response<TicketDto>

    @GET("api/tickets/check-admin")
    suspend fun checkAdmin(): Response<AdminCheckResponse>

    @GET("api/tickets/assignable-employees")
    suspend fun getAssignableEmployees(): Response<List<AssignableEmployee>>
}

