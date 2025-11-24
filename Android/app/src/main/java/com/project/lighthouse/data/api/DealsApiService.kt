package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.CreateDealRequest
import com.project.lighthouse.data.model.DealDto
import com.project.lighthouse.data.model.UpdateDealStageRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface DealsApiService {

    @GET("api/deals")
    suspend fun getDeals(): Response<List<DealDto>>

    @POST("api/deals")
    suspend fun createDeal(
        @Body request: CreateDealRequest
    ): Response<DealDto>

    @PATCH("api/deals/{dealId}")
    suspend fun updateDealStage(
        @Path("dealId") dealId: String,
        @Body request: UpdateDealStageRequest
    ): Response<DealDto>

    @DELETE("api/deals/{dealId}")
    suspend fun deleteDeal(
        @Path("dealId") dealId: String
    ): Response<Map<String, String>>
}

