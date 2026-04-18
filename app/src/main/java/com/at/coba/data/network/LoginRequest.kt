package com.at.coba.data.network

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String,
    @SerializedName("2fa_token") val two_fa_token: String? = null
)
