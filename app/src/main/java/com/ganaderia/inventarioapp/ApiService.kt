package com.ganaderia.inventarioapp

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

interface ApiService {
    @GET("animales")
    suspend fun getAllAnimales(): Response<ApiResponse<List<Animal>>>

    @GET("animales/{id}")
    suspend fun getAnimalById(@Path("id") id: Long): Response<ApiResponse<Animal>>

    @POST("animales")
    suspend fun createAnimal(@Body animal: Animal): Response<ApiResponse<Animal>>

    @PUT("animales/{id}")
    suspend fun updateAnimal(@Path("id") id: Long, @Body animal: Animal): Response<ApiResponse<Animal>>

    @DELETE("animales/{id}")
    suspend fun deleteAnimal(@Path("id") id: Long): Response<ApiResponse<Unit>>

    @POST("animales/sync")
    suspend fun syncAnimales(@Body animales: List<Animal>): Response<ApiResponse<List<Animal>>>

    companion object {
        private const val BASE_URL = "http://miganaderia.infinityfree.me/"

        fun create(): ApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}