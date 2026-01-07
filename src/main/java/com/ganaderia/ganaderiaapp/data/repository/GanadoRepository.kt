package com.ganaderia.ganaderiaapp.data.repository

import android.content.Context
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getAnimales(): Result<List<Animal>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAnimales()
            val animalesApi = response.data

            animalesApi.forEach { animalApi ->
                val localExistente = animalDao.getAnimalByIdentificacion(animalApi.identificacion)
                val entidad = animalApi.toEntity(
                    sincronizado = true,
                    localId = localExistente?.localId ?: 0
                )
                animalDao.insertarAnimal(entidad)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val listaDesdeBD = animalDao.getAllAnimales().map { it.toModel() }
        Result.success(listaDesdeBD)
    }

    suspend fun forceSync() = withContext(Dispatchers.IO) {
        try {
            val response = api.getAnimales()
            val listaAnimales = response.data

            listaAnimales.forEach { animalApi ->
                val localExistente = animalDao.getAnimalByIdentificacion(animalApi.identificacion)
                val entity = animalApi.toEntity(
                    sincronizado = true,
                    localId = localExistente?.localId ?: 0
                )
                animalDao.insertarAnimal(entity)
            }

            sincronizarKPIs()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    // CORRECCIÓN: Programar sincronización automática después de guardar offline
    suspend fun registrarAnimal(animal: AnimalRequest, context: Context? = null): Result<Animal> = withContext(Dispatchers.IO) {
        try {
            val response = api.registrarAnimal(animal)
            val nuevoAnimal = response.data
            animalDao.insertarAnimal(nuevoAnimal.toEntity(sincronizado = true))
            Result.success(nuevoAnimal)
        } catch (e: Exception) {
            // Modo offline: guardar localmente
            val entity = animal.toEntity(sincronizado = false)
            val localId = animalDao.insertarAnimal(entity).toInt()
            val animalLocal = entity.copy(localId = localId).toModel()

            // NUEVO: Programar sincronización automática si hay contexto
            context?.let { programarSincronizacion(it) }

            Result.success(animalLocal)
        }
    }

    suspend fun getAnimalByLocalId(localId: Int): Result<Animal> = withContext(Dispatchers.IO) {
        val local = animalDao.getAnimalByLocalId(localId)

        if (local != null) {
            if (!local.sincronizado) {
                return@withContext Result.success(local.toModel())
            }

            try {
                if (local.id != null) {
                    val response = api.getAnimalById(local.id)
                    val animalServidor = response.data
                    animalDao.insertarAnimal(
                        animalServidor.toEntity(sincronizado = true, localId = local.localId)
                    )
                    Result.success(animalServidor)
                } else {
                    Result.success(local.toModel())
                }
            } catch (e: Exception) {
                Result.success(local.toModel())
            }
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

    // CORRECCIÓN: Eliminar y actualizar KPIs
    suspend fun eliminarAnimalByLocalId(localId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val animal = animalDao.getAnimalByLocalId(localId)

            // Intentar borrar en API si tiene ID del servidor
            if (animal?.id != null && animal.id > 0) {
                try {
                    api.eliminarAnimal(animal.id)
                } catch (e: Exception) {
                    // Error de red, continuamos con borrado local
                    e.printStackTrace()
                }
            }

            // CORRECCIÓN: Marcar como inactivo localmente
            animalDao.eliminarPorLocalId(localId)

            // NUEVO: Actualizar KPIs después de eliminar
            sincronizarKPIs()

            Result.success(Unit)
        } catch (e: Exception) {
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

    // CORRECCIÓN: Programar sincronización después de actualizar offline
    suspend fun actualizarAnimalByLocalId(localId: Int, animal: AnimalRequest, context: Context? = null): Result<Animal> =
        withContext(Dispatchers.IO) {
            val animalExistente = animalDao.getAnimalByLocalId(localId)

            if (animalExistente == null) {
                return@withContext Result.failure(Exception("Animal no encontrado"))
            }

            try {
                if (animalExistente.id != null) {
                    val response = api.actualizarAnimal(animalExistente.id, animal)
                    val animalActualizado = response.data

                    animalDao.insertarAnimal(
                        animalActualizado.toEntity(sincronizado = true, localId = localId)
                    )
                    Result.success(animalActualizado)
                } else {
                    val entityOffline = animal.toEntity(sincronizado = false, localId = localId)
                    animalDao.insertarAnimal(entityOffline)

                    // NUEVO: Programar sincronización
                    context?.let { programarSincronizacion(it) }

                    Result.success(entityOffline.toModel())
                }
            } catch (e: Exception) {
                val entityOffline = animal.toEntity(sincronizado = false, localId = localId)
                    .copy(id = animalExistente.id)
                animalDao.insertarAnimal(entityOffline)

                // NUEVO: Programar sincronización
                context?.let { programarSincronizacion(it) }

                Result.success(entityOffline.toModel())
            }
        }

    suspend fun actualizarAnimal(id: Int, animal: AnimalRequest): Result<Animal> =
        actualizarAnimalByLocalId(id, animal)
}