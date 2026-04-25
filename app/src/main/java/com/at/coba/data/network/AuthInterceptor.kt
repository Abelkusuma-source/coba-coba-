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
            .addHeader("User-Agent", CookieManager.STOCKITY_USER_AGENT)

        if (!authToken.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization-Token", authToken)
        }
        requestBuilder.addHeader("Cookie", CookieManager.getCookieHeader())
        return chain.proceed(requestBuilder.build())
    }
}
