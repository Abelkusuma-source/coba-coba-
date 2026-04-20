package com.at.coba.data.network

import com.google.gson.annotations.SerializedName

data class OtpResponse(
    val data: OtpData?,
    val errors: List<OtpError> = emptyList(),
    val success: Boolean = false
)

data class OtpData(
    @SerializedName("2fa_token") val twoFaToken: String?
)

data class OtpError(
    val code: String,
    val context: OtpErrorContext?
)

data class OtpErrorContext(
    val message: String?
)
