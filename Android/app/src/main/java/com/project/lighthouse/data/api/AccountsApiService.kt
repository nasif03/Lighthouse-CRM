package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.AccountDto
import com.project.lighthouse.data.model.AccountDetailsResponse
import com.project.lighthouse.data.model.CreateAccountRequest
import com.project.lighthouse.data.model.UpdateAccountRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AccountsApiService {

    @GET("api/accounts")
    suspend fun getAccounts(): Response<List<AccountDto>>

    @POST("api/accounts")
    suspend fun createAccount(
        @Body request: CreateAccountRequest
    ): Response<AccountDto>

    @PUT("api/accounts/{accountId}")
    suspend fun updateAccount(
        @Path("accountId") accountId: String,
        @Body request: UpdateAccountRequest
    ): Response<AccountDto>

    @DELETE("api/accounts/{accountId}")
    suspend fun deleteAccount(
        @Path("accountId") accountId: String
    ): Response<Map<String, String>>

    @GET("api/accounts/{accountId}")
    suspend fun getAccountDetails(
        @Path("accountId") accountId: String
    ): Response<AccountDetailsResponse>
}

