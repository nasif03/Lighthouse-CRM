package com.project.lighthouse.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.project.lighthouse.config.ApiConfig
import com.project.lighthouse.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var tokenManager: TokenManager? = null

    fun initialize(tokenManager: TokenManager) {
        this.tokenManager = tokenManager
    }

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        // Use synchronous method for interceptors
        val token = tokenManager?.getTokenSync()
        
        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()
        } else {
            originalRequest.newBuilder()
                .header("Content-Type", "application/json")
                .build()
        }
        
        chain.proceed(newRequest)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val authApi: AuthApiService = retrofit.create(AuthApiService::class.java)
    val dashboardApi: DashboardApiService = retrofit.create(DashboardApiService::class.java)
    val leadsApi: LeadsApiService = retrofit.create(LeadsApiService::class.java)
    val contactsApi: ContactsApiService = retrofit.create(ContactsApiService::class.java)
    val dealsApi: DealsApiService = retrofit.create(DealsApiService::class.java)
    val accountsApi: AccountsApiService = retrofit.create(AccountsApiService::class.java)
    val organizationsApi: OrganizationsApiService = retrofit.create(OrganizationsApiService::class.java)
    val tenantsApi: TenantsApiService = retrofit.create(TenantsApiService::class.java)
}

