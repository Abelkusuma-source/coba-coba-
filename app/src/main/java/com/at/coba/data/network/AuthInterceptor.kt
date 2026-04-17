package com.at.coba.data.network

import com.at.coba.data.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val dataStoreManager: DataStoreManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val deviceId = runBlocking { dataStoreManager.getOrCreateDeviceId() }
        val authToken = runBlocking { dataStoreManager.getAuthToken().first() }
        val cookies = runBlocking { dataStoreManager.cookies.first() }

        val requestBuilder = chain.request().newBuilder()
            .addHeader("Device-Id", deviceId)
            .addHeader("Device-Type", DataStoreManager.DEVICE_TYPE)
        
        // Tambahkan Authorization jika token ada
        if (!authToken.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $authToken")
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

        // Tangkap cookie dari server jika ada (terutama untuk 2FA atau update session)
        val serverCookies = response.headers("Set-Cookie")
        if (serverCookies.isNotEmpty()) {
            val mergedCookies = serverCookies.joinToString("; ")
            runBlocking {
                dataStoreManager.setCookies(mergedCookies)
            }
        }

        return response
    }
}
