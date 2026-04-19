package com.at.coba.data.network

import android.content.Context
import com.at.coba.data.DataStoreManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object ApiClient {
    private const val BASE_URL = "https://api.stockity.id"

    fun getApiService(context: Context): ApiService {
        return buildRetrofit(context).create(ApiService::class.java)
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val dataStoreManager = DataStoreManager(context)

        val cookieJar = object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                android.util.Log.d("CookieJar", "saveFromResponse called with ${cookies.size} cookies")
                cookies.forEach {
                    android.util.Log.d("CookieJar", "Cookie: ${it.name}=${it.value}")
                }
                val cookieString = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                if (cookieString.isNotEmpty()) {
                    android.util.Log.d("CookieJar", "Saving cookies: $cookieString")
                    runBlocking {
                        dataStoreManager.setCookies(cookieString)
                    }
                    android.util.Log.d("CookieJar", "Cookies saved to DataStore")
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val cookieString = runBlocking {
                    dataStoreManager.cookies.first()
                }
                android.util.Log.d("CookieJar", "loadForRequest from DataStore: $cookieString")
                if (cookieString.isNullOrEmpty()) return emptyList()
                return cookieString.split("; ").mapNotNull { part ->
                    val pairs = part.split("=", limit = 2)
                    if (pairs.size == 2) {
                        Cookie.Builder()
                            .name(pairs[0].trim())
                            .value(pairs[1].trim())
                            .domain(url.host)
                            .build()
                    } else null
                }
            }
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor(dataStoreManager)

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
