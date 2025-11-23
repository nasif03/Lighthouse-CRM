package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.ConvertLeadResponse
import com.project.lighthouse.data.model.CreateLeadRequest
import com.project.lighthouse.data.model.LeadDto
import com.project.lighthouse.data.model.UpdateLeadStatusRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface LeadsApiService {

    @GET("api/leads")
    suspend fun getLeads(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<List<LeadDto>>

    @POST("api/leads")
    suspend fun createLead(
        @Body request: CreateLeadRequest
    ): Response<LeadDto>

    @PATCH("api/leads/{leadId}/status")
    suspend fun updateLeadStatus(
        @Path("leadId") leadId: String,
        @Body request: UpdateLeadStatusRequest
    ): Response<LeadDto>

    @POST("api/leads/{leadId}/convert")
    suspend fun convertLead(
        @Path("leadId") leadId: String
    ): Response<ConvertLeadResponse>

    @DELETE("api/leads/{leadId}")
    suspend fun deleteLead(
        @Path("leadId") leadId: String
    ): Response<Map<String, String>>
}

