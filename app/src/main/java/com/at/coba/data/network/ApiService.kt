package com.at.coba.data.network

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @POST("/passport/v2/sign_in?locale=id")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/passport/v1/2fa/validate/otp?locale=en")
    suspend fun validateOtp(@Body request: OtpRequest): OtpResponse

    /**
     * Multipart avatar upload. Path is a placeholder—align with Stockity API before production.
     */
    @Multipart
    @POST("/passport/v1/profile/avatar")
    suspend fun uploadProfileAvatar(@Part avatar: MultipartBody.Part): AvatarUploadResponse

    @retrofit2.http.GET("/platform/private/v2/profile?locale=id")
    suspend fun getProfile(): ProfileResponse

    @POST("/passport/v1/profile/update")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ProfileResponse
}
