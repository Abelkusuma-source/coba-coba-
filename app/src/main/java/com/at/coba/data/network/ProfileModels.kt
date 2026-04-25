package com.at.coba.data.network

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    val data: ProfileData
)

data class ProfileData(
    val id: String,
    val email: String,
    val phone: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name") val lastName: String?
)

data class UpdateProfileRequest(
    val phone: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("password") val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String
)

data class GenericResponse(
    val status: String?,
    val message: String?
)
