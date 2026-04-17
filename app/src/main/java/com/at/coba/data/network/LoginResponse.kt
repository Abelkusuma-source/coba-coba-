package com.at.coba.data.network

data class LoginResponse(
    val token: String,
    val is_2fa_enabled: Boolean
)
