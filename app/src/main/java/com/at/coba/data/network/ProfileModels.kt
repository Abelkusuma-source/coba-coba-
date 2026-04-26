package com.at.coba.data.network

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    val data: ProfileData
)

data class ProfileData(
    val id: Long,
    val email: String,
    val phone: String?,
    val nickname: String?,
    val gender: String?,
    val country: String?,
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name") val lastName: String?,
    @SerializedName("email_verified") val emailVerified: Boolean,
    @SerializedName("phone_verified") val phoneVerified: Boolean,
    @SerializedName("docs_verified") val docsVerified: Boolean,
    val avatar: String?
)

data class UpdateProfileRequest(
    val phone: String? = null,
    val nickname: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null
)

data class GenericResponse(
    val status: String?,
    val message: String?
)
