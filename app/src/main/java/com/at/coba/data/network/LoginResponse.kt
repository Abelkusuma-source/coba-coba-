package com.at.coba.data.network

data class LoginResponse(
    val data: LoginData
)

data class LoginData(
    val authtoken: String,
    val user_id: String,
    val is_2fa_enabled: Boolean = false // Default false jika tidak ada di JSON
)
