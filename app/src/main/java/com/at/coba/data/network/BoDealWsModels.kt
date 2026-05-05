package com.at.coba.data.network

import com.google.gson.annotations.SerializedName

/**
 * Payload Phoenix `{"topic":"bo","event":"create",...}` — selaraskan dengan Stockity web.
 */
data class BoCreateDealPayload(
    @SerializedName("created_at") val createdAtMillis: Long,
    val ric: String,
    @SerializedName("deal_type") val dealType: String,
    @SerializedName("expire_at") val expireAtEpochSeconds: Long,
    @SerializedName("option_type") val optionType: String = "turbo",
    val trend: String,
    @SerializedName("tournament_id") val tournamentId: String? = null,
    @SerializedName("is_state") val isState: Boolean = false,
    val amount: Long,
)

sealed class BoCreateDealResult {
    data class Ok(val clientRef: String, val dealUuid: String?) : BoCreateDealResult()
    data class Error(val clientRef: String, val message: String) : BoCreateDealResult()
}
