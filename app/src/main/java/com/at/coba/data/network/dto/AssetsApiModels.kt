package com.at.coba.data.network.dto

import com.google.gson.annotations.SerializedName

data class AssetsApiResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: AssetsDataEnvelope? = null,
    @SerializedName("errors") val errors: List<Any>? = null
)

data class AssetsDataEnvelope(
    @SerializedName("assets") val assets: List<AssetWire>? = null
)

data class AssetWire(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("name") val name: String? = null,
    @SerializedName("ric") val ric: String? = null
)
