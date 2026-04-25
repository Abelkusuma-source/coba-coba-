package com.at.coba.data.network

import android.content.Context
import com.at.coba.data.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val apiClientIoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

// SESUDAH ✅
object ApiClient {
    private const val BASE_URL = "https://api.stockity.id"

    fun getApiService(context: Context): ApiService {
        return buildRetrofit(context).create(ApiService::class.java)
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val dataStoreManager = DataStoreManager(context)

        val cookieJar = object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val merged = CookieManager.applyServerCookiesFromHttpResponse(cookies)
                apiClientIoScope.launch {
                    dataStoreManager.setCookies(merged)
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return CookieManager.cookiesForHttpUrl(url)
            }
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor()

        val okHttpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(authInterceptor)
            .addNetworkInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
