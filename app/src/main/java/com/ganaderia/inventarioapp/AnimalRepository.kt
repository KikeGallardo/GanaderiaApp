package com.ganaderia.inventarioapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.flow.Flow

class AnimalRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val animalDao = database.animalDao()
    private val apiService = ApiService.create()

    private val TAG = "AnimalRepository"

    // Operaciones locales
    fun getAllAnimales(): Flow<List<Animal>> = animalDao.getAllAnimales()

    suspend fun getAnimalById(id: Long): Animal? = animalDao.getAnimalById(id)

    suspend fun getAnimalesEliminados(): List<Animal> = animalDao.getAnimalesEliminados()

    suspend fun insertAnimal(animal: Animal): Long {
        val id = animalDao.insertAnimal(animal.copy(sincronizado = false))
        return id
    }

    suspend fun updateAnimal(animal: Animal) {
        animalDao.updateAnimal(animal.copy(sincronizado = false))
    }

    suspend fun deleteAnimal(animal: Animal) {
        animalDao.deleteAnimal(animal)
    }

    suspend fun marcarComoEliminado(animal: Animal) {
        animalDao.updateAnimal(animal.copy(eliminado = true, sincronizado = false))
    }

    suspend fun restaurarAnimal(animal: Animal) {
        animalDao.updateAnimal(animal.copy(eliminado = false, sincronizado = false))
    }

    suspend fun eliminarPermanentemente(animal: Animal) {
        animalDao.deleteAnimal(animal)
    }

    suspend fun vaciarPapelera() {
        animalDao.deleteAllEliminados()
    }

    // Sincronización con servidor
    suspend fun subirCambios(): Result<String> {
        if (!hayInternet()) {
            return Result.failure(Exception("No hay conexión a internet"))
        }

        return try {
            // Obtener animales no sincronizados
            val animalesNoSinc = animalDao.getAnimalesNoSincronizados()

            if (animalesNoSinc.isEmpty()) {
                return Result.success("No hay cambios pendientes para subir")
            }

            Log.d(TAG, "Subiendo ${animalesNoSinc.size} animales al servidor...")

            val response = apiService.syncAnimales(animalesNoSinc)

            if (response.isSuccessful && response.body()?.success == true) {
                // Marcar como sincronizados
                animalesNoSinc.forEach { animal ->
                    animalDao.marcarComoSincronizado(animal.id)
                }

                Result.success("${animalesNoSinc.size} cambios subidos exitosamente")
            } else {
                val errorMsg = response.body()?.message ?: "Error desconocido"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al subir cambios: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun descargarDatos(): Result<String> {
        if (!hayInternet()) {
            return Result.failure(Exception("No hay conexión a internet"))
        }

        return try {
            Log.d(TAG, "Descargando datos del servidor...")

            val response = apiService.getAllAnimales()

            if (response.isSuccessful && response.body()?.success == true) {
                val animalesServidor = response.body()?.data ?: emptyList()

                if (animalesServidor.isEmpty()) {
                    return Result.success("No hay datos en el servidor")
                }

                // Limpiar base de datos local
                animalDao.deleteAll()

                // Insertar datos descargados
                animalDao.insertAnimales(animalesServidor.map {
                    it.copy(sincronizado = true)
                })

                Result.success("${animalesServidor.size} registros descargados exitosamente")
            } else {
                val errorMsg = response.body()?.message ?: "Error desconocido"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al descargar datos: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun sincronizarBidireccional(): Result<String> {
        if (!hayInternet()) {
            return Result.failure(Exception("No hay conexión a internet"))
        }

        return try {
            Log.d(TAG, "Iniciando sincronización bidireccional...")

            // 1. Subir cambios locales
            val subirResult = subirCambios()
            if (subirResult.isFailure) {
                return subirResult
            }

            // 2. Descargar datos del servidor
            val descargarResult = descargarDatos()
            if (descargarResult.isFailure) {
                return descargarResult
            }

            Result.success("Sincronización completa exitosa")
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización bidireccional: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun hayInternet(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}