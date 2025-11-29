package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.CreateEmployeeRequest
import com.project.lighthouse.data.model.EmployeeDto
import com.project.lighthouse.data.model.UpdateEmployeeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface EmployeesApiService {
    @GET("api/organizations/{org_id}/employees")
    suspend fun getEmployees(@Path("org_id") orgId: String): Response<List<EmployeeDto>>

    @POST("api/organizations/{org_id}/employees")
    suspend fun createEmployee(
        @Path("org_id") orgId: String,
        @Body request: CreateEmployeeRequest
    ): Response<EmployeeDto>

    @PUT("api/organizations/{org_id}/employees/{employee_id}")
    suspend fun updateEmployee(
        @Path("org_id") orgId: String,
        @Path("employee_id") employeeId: String,
        @Body request: UpdateEmployeeRequest
    ): Response<EmployeeDto>
}

