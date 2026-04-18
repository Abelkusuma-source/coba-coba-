package com.at.coba.data.network

import com.at.coba.data.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val dataStoreManager: DataStoreManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val deviceId = runBlocking { dataStoreManager.getOrCreateDeviceId() }
        val authToken = runBlocking { dataStoreManager.authToken.first() }
        val cookies = runBlocking { dataStoreManager.cookies.first() }
        android.util.Log.d("AuthInterceptor", "Cookie yang akan dikirim: $cookies")

        val requestBuilder = chain.request().newBuilder()
            .addHeader("Device-Id", deviceId)
            .addHeader("Device-Type", DataStoreManager.DEVICE_TYPE)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36")
        // Tambahkan Authorization jika token ada
        if (!authToken.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization-Token",authToken)
        }

        // Tambahkan Cookie. Jika ada cookie dari server gunakan itu, 
        // jika tidak gunakan format default device_id dan device_type
        val cookieHeader = if (!cookies.isNullOrEmpty()) {
            cookies
        } else {
            "device_id=$deviceId; device_type=${DataStoreManager.DEVICE_TYPE}"
        }
        requestBuilder.addHeader("Cookie", cookieHeader)

        val response = chain.proceed(requestBuilder.build())



        return response
    }
}
