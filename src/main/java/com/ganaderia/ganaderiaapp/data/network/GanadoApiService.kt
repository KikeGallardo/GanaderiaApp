package com.ganaderia.ganaderiaapp.data.network

import com.ganaderia.ganaderiaapp.data.model.*
import retrofit2.http.*

interface GanadoApiService {

    @GET("api/kpis")
    suspend fun getKPIs(): KPIs // Quitamos el ApiResponse

    @GET("api/animales")
    suspend fun getAnimales(
        @Query("busqueda") busqueda: String? = null,
        @Query("raza") raza: String? = null,
        @Query("sexo") sexo: String? = null,
        @Query("categoria") categoria: String? = null
    ): ApiResponse<List<Animal>>

    @GET("api/animales/{id}")
    suspend fun getAnimalById(@Path("id") id: Int): ApiResponse<Animal>

    @GET("api/animales/hembras")
    suspend fun getHembras(): ApiResponse<List<AnimalSimple>>

    @GET("api/animales/machos")
    suspend fun getMachos(): ApiResponse<List<AnimalSimple>>

    // --- CAMBIO AQUÍ: Nombre sincronizado con el Repositorio ---
    @POST("api/animales")
    suspend fun registrarAnimal(@Body animal: AnimalRequest): ApiResponse<Animal>

    @PUT("api/animales/{id}")
    suspend fun actualizarAnimal(
        @Path("id") id: Int,
        @Body request: AnimalRequest
    ): ApiResponse<Animal>

    @DELETE("api/animales/{id}")
    suspend fun eliminarAnimal(@Path("id") id: Int): ApiResponse<Any>

    @GET("api/animales/{id}/vacunas")
    suspend fun getVacunas(@Path("id") animalId: Int): ApiResponse<List<Vacuna>>

    @POST("api/vacunas")
    suspend fun registrarVacuna(@Body vacuna: VacunaRequest): ApiResponse<Any>

    @DELETE("api/vacunas/{id}")
    suspend fun eliminarVacuna(@Path("id") id: Int): ApiResponse<Any>

    @GET("api/catalogo-vacunas")
    suspend fun getCatalogo(): ApiResponse<List<String>>

    @POST("api/catalogo-vacunas")
    suspend fun guardarCatalogo(@Body body: Map<String, String>): ApiResponse<Any>

}