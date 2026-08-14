package com.example.data.api

import com.example.data.repository.SettingsRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(private val settingsRepository: SettingsRepository) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    private var currentBaseUrl: String = ""
    private var currentApi: CloudMoviesApi? = null

    fun getApi(): CloudMoviesApi {
        val baseUrl = settingsRepository.getBaseUrl()
        if (currentApi == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            currentApi = retrofit.create(CloudMoviesApi::class.java)
        }
        return currentApi!!
    }

    fun getFullStreamUrl(slug: String, server: String? = null): String {
        val base = settingsRepository.getBaseUrl().trimEnd('/')
        val serverParam = if (!server.isNullOrBlank()) "?server=$server" else ""
        return "$base/stream/$slug$serverParam"
    }

    fun getPlayUrl(slug: String, server: String? = null): String {
        val base = settingsRepository.getBaseUrl().trimEnd('/')
        val serverParam = if (!server.isNullOrBlank()) "?server=$server" else ""
        return "$base/play/$slug$serverParam"
    }
}
