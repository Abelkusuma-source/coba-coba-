package com.at.coba.data.network

import com.google.gson.annotations.SerializedName

data class AvatarUploadResponse(
    val data: AvatarUploadData? = null
)

data class AvatarUploadData(
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null
) {
    fun resolvedUrl(): String? = url?.takeIf { it.isNotBlank() } ?: avatarUrl?.takeIf { it.isNotBlank() }
}
