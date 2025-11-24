package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.ContactDto
import com.project.lighthouse.data.model.CreateContactRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ContactsApiService {

    @GET("api/contacts")
    suspend fun getContacts(): Response<List<ContactDto>>

    @POST("api/contacts")
    suspend fun createContact(
        @Body request: CreateContactRequest
    ): Response<ContactDto>

    @DELETE("api/contacts/{contactId}")
    suspend fun deleteContact(
        @Path("contactId") contactId: String
    ): Response<Map<String, String>>
}

