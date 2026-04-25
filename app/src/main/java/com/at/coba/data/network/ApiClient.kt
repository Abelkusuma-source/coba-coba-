package com.at.coba.data.network

import android.content.Context
import com.at.coba.BuildConfig
import com.at.coba.data.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://api.stockity.id"

    fun getApiService(context: Context): ApiService {
        return buildRetrofit(context).create(ApiService::class.java)
    }

    /** Shared client (cookies + [AuthInterceptor]) for image download and Retrofit. */
    fun getOkHttpClient(context: Context): OkHttpClient {
        return buildOkHttpClient(DataStoreManager(context.applicationContext))
    }

    private fun buildRetrofit(context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun buildOkHttpClient(dataStoreManager: DataStoreManager): OkHttpClient {
        val cookieJar = object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val sessionCookie = cookies.firstOrNull { it.name == "SESSION" }
                val otherCookies = cookies.filter { it.name != "SESSION" }

                if (sessionCookie != null) {
                    runBlocking { dataStoreManager.setSessionCookie("SESSION=${sessionCookie.value}") }
                }

                val cookieString = otherCookies.joinToString("; ") { "${it.name}=${it.value}" }
                if (cookieString.isNotEmpty()) {
                    runBlocking { dataStoreManager.setCookies(cookieString) }
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val cookieString = runBlocking { dataStoreManager.cookies.first() }
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
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val authInterceptor = AuthInterceptor(dataStoreManager)

        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(authInterceptor)
            .addNetworkInterceptor(loggingInterceptor)
            .build()
    }
}
