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
        
        val requestBuilder = originalRequest.newBuilder()
            .header("Content-Type", "application/json")
            // Add ngrok-skip-browser-warning header if using ngrok
            .header("ngrok-skip-browser-warning", "true")
        
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        
        val newRequest = requestBuilder.build()
        
        chain.proceed(newRequest)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
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
    val gmailApi: GmailApiService = retrofit.create(GmailApiService::class.java)
    val meetingsApi: MeetingsApiService = retrofit.create(MeetingsApiService::class.java)
    val jiraApi: JiraApiService = retrofit.create(JiraApiService::class.java)
    val ticketsApi: TicketsApiService = retrofit.create(TicketsApiService::class.java)
    val chatApi: ChatApiService = retrofit.create(ChatApiService::class.java)
    val supportChatApi: SupportChatApiService = retrofit.create(SupportChatApiService::class.java)
    val calendarApi: CalendarApiService = retrofit.create(CalendarApiService::class.java)
    val firefliesApi: FirefliesApiService = retrofit.create(FirefliesApiService::class.java)
    val employeesApi: EmployeesApiService = retrofit.create(EmployeesApiService::class.java)
    val rolesApi: RolesApiService = retrofit.create(RolesApiService::class.java)
}

