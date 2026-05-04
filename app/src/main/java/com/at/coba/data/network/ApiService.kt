package com.at.coba.data.network

import okhttp3.MultipartBody
import com.at.coba.data.network.dto.AssetsApiResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {
    @POST("/passport/v2/sign_in?locale=id")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/passport/v1/2fa/validate/otp?locale=id")
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

    /**
     * Riwayat transaksi boil deal (demo/real).
     */
    @GET("/bo-deals-history/v3/deals/trade")
    suspend fun getTradeDealsRaw(
        @Query("type") type: String,
        @Query("locale") locale: String = "id"
    ): ResponseBody

    @GET("/bo-assets/v6/assets")
    suspend fun getAssets(
        @Query("locale") locale: String = "id"
    ): AssetsApiResponse
}
