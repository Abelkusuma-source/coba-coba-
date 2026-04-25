package com.at.coba.data.network

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val data: LoginData
)

data class LoginData(
    val authtoken: String,
    val user_id: String,
    val is_2fa_enabled: Boolean = false,
    /** Populated when backend includes avatar URLs in sign-in payload. */
    val avatar_url: String? = null,
    @SerializedName("profile_photo_url") val profile_photo_url: String? = null,
    @SerializedName("photo_url") val photo_url: String? = null
)
