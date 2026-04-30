package com.at.coba.data.network

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    val data: ProfileData
)

data class ProfileData(
    val id: Long = 0,
    val email: String = "",
    val phone: String? = null,
    val nickname: String? = null,
    val gender: String? = null,
    val country: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("email_verified") val emailVerified: Boolean = false,
    @SerializedName("phone_verified") val phoneVerified: Boolean = false,
    @SerializedName("docs_verified") val docsVerified: Boolean = false,
    val avatar: String? = null,
    /** Server may send `yyyy-MM-dd` or empty string when unset. */
    val birthday: String? = null
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
