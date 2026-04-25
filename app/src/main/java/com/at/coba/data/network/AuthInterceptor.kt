package com.at.coba.data.network

import com.at.coba.data.DataStoreManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val deviceId = CookieManager.getDeviceId()
        val authToken = CookieManager.getAuthToken()
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Device-Id", deviceId)
            .addHeader("Device-Type", DataStoreManager.DEVICE_TYPE)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36")

        if (!authToken.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization-Token", authToken)
        }
        requestBuilder.addHeader("Cookie", CookieManager.getCookieHeader())
        return chain.proceed(requestBuilder.build())
    }
}
