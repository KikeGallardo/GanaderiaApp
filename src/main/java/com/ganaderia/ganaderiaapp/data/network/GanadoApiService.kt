package com.ganaderia.ganaderiaapp.data.network

import com.ganaderia.ganaderiaapp.data.model.*
import retrofit2.http.*

interface GanadoApiService {

    // En tu interface ApiService
    @GET("api/kpis")
    suspend fun getKPIs(): KPIs // Devuelve KPIs directamente, no ApiResponse<KPIs>

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

    @POST("api/animales")
    suspend fun crearAnimal(@Body animal: AnimalRequest): ApiResponse<Any>

    @PUT("api/animales/{id}")
    suspend fun actualizarAnimal(
        @Path("id") id: Int,
        @Body animal: AnimalRequest
    ): ApiResponse<Any>

    @DELETE("api/animales/{id}")
    suspend fun eliminarAnimal(@Path("id") id: Int): ApiResponse<Any>

    @GET("api/animales/{id}/vacunas")
    suspend fun getVacunas(@Path("id") animalId: Int): ApiResponse<List<Vacuna>>

    @POST("api/vacunas")
    suspend fun registrarVacuna(@Body vacuna: VacunaRequest): ApiResponse<Any>

    @DELETE("api/vacunas/{id}")
    suspend fun eliminarVacuna(@Path("id") id: Int): ApiResponse<Any>

    @GET("api/catalogo-vacunas")
    suspend fun getCatalogoVacunas(): retrofit2.Response<ApiResponse<List<String>>>
    @POST("api/catalogo-vacunas")
    suspend fun guardarEnCatalogo(@Body body: Map<String, String>): retrofit2.Response<Unit>
}
