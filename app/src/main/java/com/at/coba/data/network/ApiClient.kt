package com.at.coba.data.network

import android.content.Context
import com.at.coba.data.DataStoreManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// SESUDAH ✅
object ApiClient {
    private const val BASE_URL = "https://api.stockity.id"

    fun getApiService(context: Context): ApiService {
        return buildRetrofit(context).create(ApiService::class.java)
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val dataStoreManager = DataStoreManager(context)
        
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor(dataStoreManager)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
