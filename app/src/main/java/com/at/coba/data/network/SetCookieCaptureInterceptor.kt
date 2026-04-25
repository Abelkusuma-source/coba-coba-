package com.at.coba.data.network

import com.at.coba.data.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor yang tugasnya menangkap header Set-Cookie dari response HTTP dan menyimpan cookie session
 */
class SetCookieCaptureInterceptor(
    private val dataStoreManager: DataStoreManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val lines = response.headers.values("Set-Cookie")
        if (lines.isNotEmpty()) {
            val merged = CookieManager.persistSetCookieLines(request.url, lines)
            runBlocking(Dispatchers.IO) {
                dataStoreManager.setCookies(merged)
            }
        }
        return response
    }
}
