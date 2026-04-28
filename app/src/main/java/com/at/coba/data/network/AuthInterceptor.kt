package com.at.coba.data.network

import com.at.coba.data.DataStoreManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Header wajib untuk REST (termasuk jalur `/platform/private/…`): `Device-Id`, `Device-Type`,
 * dan `Authorization-Token` (nilai mentah seperti di browser, tanpa prefix `Bearer `).
 * Cookie/session tetap lewat [CookieJar] + interceptor cookie lainnya.
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val deviceId = CookieManager.getDeviceIdForHeader()
        val authToken = CookieManager.getAuthorizationTokenForHeader()

        val requestBuilder = chain.request().newBuilder()
            .header("Device-Id", deviceId)
            .header("Device-Type", DataStoreManager.DEVICE_TYPE)
            .header("User-Agent", CookieManager.STOCKITY_USER_AGENT)
            .header("Accept", "application/json")
            .header("X-Requested-With", "XMLHttpRequest")

        if (authToken != null) {
            requestBuilder.header("Authorization-Token", authToken)
        }

        return chain.proceed(requestBuilder.build())
    }
}
