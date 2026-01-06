package com.ganaderia.ganaderiaapp.data.repository

import com.ganaderia.ganaderiaapp.data.model.*
import com.ganaderia.ganaderiaapp.data.network.RetrofitClient
import com.ganaderia.ganaderiaapp.data.network.GanadoApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GanadoRepository {
    private val api = RetrofitClient.api

    // ==================== KPIs ====================
    suspend fun getKPIs(): Result<KPIs> = withContext(Dispatchers.IO) {
        try {
            val response = api.getKPIs()
            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("REPO_ERROR", "Fallo en KPIs: ${e.message}")
            Result.failure(e)
        }
    }

    // ==================== ANIMALES ====================
    suspend fun getAnimales(
        busqueda: String? = null,
        raza: String? = null,
        sexo: String? = null,
        categoria: String? = null
    ): Result<List<Animal>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAnimales(busqueda, raza, sexo, categoria)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Error al obtener animales"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAnimalById(id: Int): Result<Animal> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAnimalById(id)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Animal no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHembras(): Result<List<AnimalSimple>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getHembras()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Error al obtener hembras"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMachos(): Result<List<AnimalSimple>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMachos()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Error al obtener machos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun crearAnimal(animal: AnimalRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.crearAnimal(animal)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception(response.message ?: "Error al crear animal"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarAnimal(id: Int, animal: AnimalRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.actualizarAnimal(id, animal)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception(response.message ?: "Error al actualizar animal"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarAnimal(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.eliminarAnimal(id)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception(response.message ?: "Error al eliminar animal"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== VACUNAS ====================
    suspend fun getVacunas(animalId: Int): Result<List<Vacuna>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getVacunas(animalId)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Error al obtener vacunas"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registrarVacuna(vacuna: VacunaRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.registrarVacuna(vacuna)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception(response.message ?: "Error al registrar vacuna"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarVacuna(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.eliminarVacuna(id)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception(response.message ?: "Error al eliminar vacuna"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== CATÁLOGO DINÁMICO ====================

    // Unificamos a un solo nombre: obtenerCatalogoVacunas
    // GanadoRepository.kt
    suspend fun obtenerCatalogoVacunas(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCatalogoVacunas()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Error 404: Ruta no encontrada en el servidor"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Dejamos UNA SOLA versión de guardarEnCatalogo para evitar el Conflicting Overloads
    suspend fun guardarEnCatalogo(nombre: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.guardarEnCatalogo(mapOf("nombre" to nombre))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al guardar: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}