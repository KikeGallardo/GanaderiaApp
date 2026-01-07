package com.ganaderia.ganaderiaapp.data.repository

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ganaderia.ganaderiaapp.data.model.*
import com.ganaderia.ganaderiaapp.data.network.GanadoApiService
import com.ganaderia.ganaderiaapp.data.local.dao.VacunaDao
import com.ganaderia.ganaderiaapp.data.local.dao.AnimalDao
import com.ganaderia.ganaderiaapp.data.local.entities.AnimalEntity
import com.ganaderia.ganaderiaapp.data.local.dao.KpiDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class GanadoRepository(
    private val api: GanadoApiService,
    private val animalDao: AnimalDao,
    private val vacunaDao: VacunaDao,
    private val kpiDao: KpiDao
) {

    suspend fun getKPIs(): Flow<KPIs> = flow {
        val local = kpiDao.getKPIs().firstOrNull()
        if (local != null) {
            emit(local.toDomain())
        }

        try {
            val remoto = api.getKPIs()
            kpiDao.insertKPIs(remoto.toEntity())
            emit(remoto)
        } catch (e: Exception) {
            if (local == null) {
                throw e
            }
        }
    }

    fun getKPIsLocales(): Flow<KPIs?> {
        return kpiDao.getKPIs().map { it?.toDomain() }
    }

    suspend fun sincronizarKPIs() {
        try {
            val remoto = api.getKPIs()
            kpiDao.insertKPIs(remoto.toEntity())
            Log.d("GanadoRepository", "KPIs sincronizados correctamente")
        } catch (e: Exception) {
            Log.e("GanadoRepository", "Error sincronizando KPIs", e)
            e.printStackTrace()
        }
    }

    suspend fun getAnimales(): Result<List<Animal>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAnimales()
            val animalesApi = response.data

            animalesApi.forEach { animalApi ->
                val localExistente = animalDao.getAnimalByServerId(animalApi.id)
                val entidad = animalApi.toEntity(
                    sincronizado = true,
                    localId = localExistente?.localId ?: 0
                )
                animalDao.insertarAnimal(entidad)
            }
            Log.d("GanadoRepository", "Sincronizados ${animalesApi.size} animales desde API")
        } catch (e: Exception) {
            Log.e("GanadoRepository", "Error obteniendo animales de API", e)
            e.printStackTrace()
        }

        val listaDesdeBD = animalDao.getAllAnimales().map { it.toModel() }
        Result.success(listaDesdeBD)
    }

    suspend fun forceSync() = withContext(Dispatchers.IO) {
        try {
            // 1. Sincronizar animales no sincronizados primero
            sincronizarAnimalesPendientes()

            // 2. Obtener todos los animales del servidor
            val response = api.getAnimales()
            val listaAnimales = response.data

            listaAnimales.forEach { animalApi ->
                val localExistente = animalDao.getAnimalByServerId(animalApi.id)
                val entity = animalApi.toEntity(
                    sincronizado = true,
                    localId = localExistente?.localId ?: 0
                )
                animalDao.insertarAnimal(entity)
            }

            // 3. Actualizar KPIs
            sincronizarKPIs()

            Log.d("GanadoRepository", "Sincronización forzada completada")
        } catch (e: Exception) {
            Log.e("GanadoRepository", "Error en sincronización forzada", e)
            e.printStackTrace()
            throw e
        }
    }

    private suspend fun sincronizarAnimalesPendientes() = withContext(Dispatchers.IO) {
        try {
            val noSincronizados = animalDao.getNoSincronizados()
            Log.d("GanadoRepository", "Sincronizando ${noSincronizados.size} animales pendientes")

            noSincronizados.forEach { animalLocal ->
                try {
                    val request = animalLocal.toRequest()

                    // Si el animal tiene ID del servidor, actualizar; si no, crear
                    if (animalLocal.id != null && animalLocal.id > 0) {
                        Log.d("GanadoRepository", "Actualizando animal en servidor: ${animalLocal.identificacion} (ID servidor: ${animalLocal.id})")
                        val response = api.actualizarAnimal(animalLocal.id, request)
                        if (response.success) {
                            val entidadActualizada = animalLocal.copy(sincronizado = true)
                            animalDao.insertarAnimal(entidadActualizada)
                        }
                    } else {
                        Log.d("GanadoRepository", "Creando nuevo animal en servidor: ${animalLocal.identificacion}")
                        val response = api.registrarAnimal(request)
                        if (response.success) {
                            val animalServidor = response.data
                            val entidadActualizada = animalLocal.copy(
                                id = animalServidor.id,
                                sincronizado = true
                            )
                            animalDao.insertarAnimal(entidadActualizada)
                            Log.d("GanadoRepository", "Animal ${animalLocal.identificacion} sincronizado con ID ${animalServidor.id}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GanadoRepository", "Error sincronizando animal ${animalLocal.identificacion}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("GanadoRepository", "Error en sincronizarAnimalesPendientes", e)
        }
    }

    suspend fun registrarAnimal(animal: AnimalRequest, context: Context? = null): Result<Animal> = withContext(Dispatchers.IO) {
        try {
            Log.d("GanadoRepository", "Intentando registrar animal: ${animal.identificacion}")
            val response = api.registrarAnimal(animal)
            val nuevoAnimal = response.data

            val entity = nuevoAnimal.toEntity(sincronizado = true)
            animalDao.insertarAnimal(entity)

            sincronizarKPIs()

            Log.d("GanadoRepository", "Animal registrado exitosamente con ID ${nuevoAnimal.id}")
            Result.success(nuevoAnimal)
        } catch (e: Exception) {
            Log.e("GanadoRepository", "Error registrando animal, guardando offline", e)
            val entity = animal.toEntity(sincronizado = false)
            val localId = animalDao.insertarAnimal(entity).toInt()
            val animalLocal = entity.copy(localId = localId).toModel()

            context?.let {
                programarSincronizacion(it)
                Log.d("GanadoRepository", "Sincronización programada para animal offline")
            }

            Result.success(animalLocal)
        }
    }

    suspend fun getAnimalByLocalId(localId: Int): Result<Animal> = withContext(Dispatchers.IO) {
        val local = animalDao.getAnimalByLocalId(localId)

        if (local != null) {
            // Si ya está marcado como sincronizado localmente, intentamos refrescar datos
            try {
                if (local.id != null && local.id > 0) {
                    val response = api.getAnimalById(local.id)
                    if (response.success) {
                        val animalServidor = response.data
                        // IMPORTANTE: Aquí forzamos sincronizado = true al guardar el refresco
                        animalDao.insertarAnimal(
                            animalServidor.toEntity(sincronizado = true, localId = local.localId)
                        )
                        return@withContext Result.success(animalServidor)
                    }
                }
            } catch (e: Exception) {
                Log.e("GanadoRepository", "Error refrescando desde servidor, usando local", e)
            }

            // Si la API falla o el animal no tiene ID de servidor aún, devolvemos el local
            Result.success(local.toModel())
        } else {
            Result.failure(Exception("Animal no encontrado"))
        }
    }

    suspend fun getAnimalById(id: Int): Result<Animal> = getAnimalByLocalId(id)

    suspend fun registrarAnimalApiDirecto(animal: AnimalRequest): Result<Animal> = withContext(Dispatchers.IO) {
        try {
            val response = api.registrarAnimal(animal)
            if (response.success) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarAnimalLocal(entidad: AnimalEntity) = withContext(Dispatchers.IO) {
        animalDao.insertarAnimal(entidad)
    }

    suspend fun getAnimalesNoSincronizados(): List<AnimalEntity> = withContext(Dispatchers.IO) {
        animalDao.getAnimalesPorEstadoSincronizacion(false)
    }

    fun programarSincronizacion(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_ganado",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )

        Log.d("GanadoRepository", "Worker de sincronización programado")
    }

    suspend fun getAnimalesSinFiltros(): Result<List<Animal>> {
        return getAnimales()
    }

    suspend fun getVacunas(animalId: Int): Result<List<Vacuna>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getVacunas(animalId)
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerCatalogoVacunas(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCatalogo()
            Result.success(response.data ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun guardarEnCatalogo(nombre: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.guardarCatalogo(mapOf("nombre" to nombre))
            if (response.success) Result.success(Unit) else Result.failure(Exception(response.message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarVacuna(vacunaId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.eliminarVacuna(vacunaId)
            if (response.success) Result.success(Unit) else Result.failure(Exception(response.message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarAnimalByLocalId(localId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val animal = animalDao.getAnimalByLocalId(localId)

            if (animal == null) {
                Log.e("GanadoRepository", "Animal no encontrado con localId: $localId")
                return@withContext Result.failure(Exception("Animal no encontrado"))
            }

            Log.d("GanadoRepository", "Eliminando animal - localId: ${animal.localId}, serverId: ${animal.id}, identificacion: ${animal.identificacion}")

            // Solo intentar borrar en API si:
            // 1. Tiene ID del servidor (id > 0)
            // 2. Está sincronizado (para evitar intentar borrar algo que nunca se subió)
            if (animal.id != null && animal.id > 0 && animal.sincronizado) {
                try {
                    Log.d("GanadoRepository", "Intentando eliminar del servidor con ID: ${animal.id}")
                    val response = api.eliminarAnimal(animal.id)
                    Log.d("GanadoRepository", "Animal eliminado del servidor exitosamente")
                } catch (e: Exception) {
                    // Si falla el borrado en servidor (404, 500, etc), continuamos con borrado local
                    // El 404 significa que ya no existe en servidor, así que está bien
                    Log.w("GanadoRepository", "Error eliminando del servidor (posiblemente ya eliminado): ${e.message}")
                }
            } else {
                Log.d("GanadoRepository", "Animal no sincronizado o sin ID servidor, solo se eliminará localmente")
            }

            // Siempre marcar como inactivo localmente
            animalDao.eliminarPorLocalId(localId)
            Log.d("GanadoRepository", "Animal marcado como inactivo localmente")

            // Actualizar KPIs
            sincronizarKPIs()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GanadoRepository", "Error eliminando animal", e)
            Result.failure(e)
        }
    }

    suspend fun eliminarAnimal(id: Int): Result<Unit> = eliminarAnimalByLocalId(id)

    suspend fun registrarVacuna(request: VacunaRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.registrarVacuna(request)
            vacunaDao.insertVacuna(request.toEntity(sincronizado = response.success))
            Result.success(Unit)
        } catch (e: Exception) {
            vacunaDao.insertVacuna(request.toEntity(sincronizado = false))
            Result.success(Unit)
        }
    }

    suspend fun actualizarAnimalByLocalId(
        localId: Int,
        animal: AnimalRequest,
        context: Context? = null
    ): Result<Animal> = withContext(Dispatchers.IO) {
        val animalExistente = animalDao.getAnimalByLocalId(localId)

        try {
            if (animalExistente?.id != null && animalExistente.id > 0) {
                val response = api.actualizarAnimal(animalExistente.id, animal)
                if (response.success) {
                    val animalActualizado = response.data

                    // CORRECCIÓN: Pasar explícitamente el localId que estamos editando
                    val entidadParaGuardar = animalActualizado.toEntity(
                        sincronizado = true,
                        localId = localId // <--- El ID que viene del parámetro de la función del repositorio
                    )

                    animalDao.insertarAnimal(entidadParaGuardar)
                    sincronizarKPIs()
                    return@withContext Result.success(animalActualizado)
                } else {
                    Result.failure(Exception(response.message))
                }
            } else {
                // Lógica para animales que aún no tienen ID de servidor...
                Result.failure(Exception("ID de servidor no encontrado"))
            }
        } catch (e: Exception) {
            // En caso de error de red, marcar como NO sincronizado para el SyncWorker
            val entityOffline = animal.toEntity(sincronizado = false, localId = localId)
                .copy(id = animalExistente?.id)
            animalDao.insertarAnimal(entityOffline)
            Result.success(entityOffline.toModel())
        }
    }

    suspend fun actualizarAnimal(id: Int, animal: AnimalRequest, context: Context? = null): Result<Animal> =
        actualizarAnimalByLocalId(id, animal, context)
}