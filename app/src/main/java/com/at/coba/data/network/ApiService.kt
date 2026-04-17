package com.at.coba.data.network

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/passport/v2/sign_in?locale=en")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}
