package com.example.data.remote

import com.example.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {

    @GET("/")
    suspend fun healthCheck(): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("/create")
    suspend fun createUser(@Body request: CreateUserRequest): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("/create/payment")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("/add/budget")
    suspend fun addBudget(@Body request: AddBudgetRequest): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("/get/account")
    suspend fun getAccount(@Body request: UsernameOnlyRequest): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("/get/txn")
    suspend fun getTransactions(@Body request: UsernameOnlyRequest): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("/get/budget")
    suspend fun getBudget(@Body request: UsernameOnlyRequest): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("/update/budget")
    suspend fun updateBudget(@Body request: UpdateBudgetRequest): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("/get/txn/statistics")
    suspend fun getTxnStatistics(@Body request: UsernameOnlyRequest): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("/get/monthly/txn")
    suspend fun getMonthlyTxn(@Body request: MonthlyTxnRequest): Response<ResponseBody>

    @GET("/send/report/")
    suspend fun sendReport(): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("/get/qr")
    suspend fun getQr(@Body request: GetQrRequest): Response<ResponseBody>
}
