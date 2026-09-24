package com.moneyprinter.turbo.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface OpenAiCompatibleApi {
    @POST("v1/chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body body: ChatRequest
    ): ChatResponse
}

interface PexelsApi {
    @GET("videos/search")
    suspend fun searchVideos(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 15,
        @Query("orientation") orientation: String = "portrait"
    ): PexelsVideoResponse
}

object ApiClients {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    fun openAiCompatible(baseUrl: String): OpenAiCompatibleApi {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAiCompatibleApi::class.java)
    }

    val pexels: PexelsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.pexels.com/v1/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PexelsApi::class.java)
    }
}
