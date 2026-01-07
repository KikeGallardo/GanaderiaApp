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
            // 🔧 CORRECCIÓN: Sincronizar pendientes ANTES de traer datos del servidor
            Log.d("GanadoRepository", "Sincronizando cambios pendientes antes de traer datos del servidor...")
            sincronizarAnimalesPendientes()

            val response = api.getAnimales()
            val animalesApi = response.data

            animalesApi.forEach { animalApi ->
                val localExistente = animalDao.getAnimalByServerId(animalApi.id)

                // 🔧 CORRECCIÓN: Solo sobrescribir si está sincronizado
                // Si existe local y NO está sincronizado, significa que tiene cambios pendientes
                if (localExistente != null && !localExistente.sincronizado) {
                    Log.d("GanadoRepository", "⚠️ Animal ${animalApi.identificacion} tiene cambios pendientes, NO sobrescribir")
                    return@forEach // Saltar este animal
                }

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
            // 1. 🔧 CORRECCIÓN: Sincronizar animales no sincronizados PRIMERO
            Log.d("GanadoRepository", "=== FORCE SYNC: Sincronizando pendientes primero ===")
            sincronizarAnimalesPendientes()

            // 2. Obtener todos los animales del servidor
            Log.d("GanadoRepository", "=== FORCE SYNC: Trayendo datos del servidor ===")
            val response = api.getAnimales()
            val listaAnimales = response.data

            listaAnimales.forEach { animalApi ->
                val localExistente = animalDao.getAnimalByServerId(animalApi.id)

                // 🔧 CORRECCIÓN: Solo sobrescribir si está sincronizado
                if (localExistente != null && !localExistente.sincronizado) {
                    Log.d("GanadoRepository", "⚠️ Animal ${animalApi.identificacion} tiene cambios pendientes, NO sobrescribir")
                    return@forEach
                }

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
            Log.d("GanadoRepository", "=== SINCRONIZANDO ${noSincronizados.size} ANIMALES PENDIENTES ===")

            noSincronizados.forEach { animalLocal ->
                try {
                    val request = animalLocal.toRequest()

                    // Si el animal tiene ID del servidor, actualizar; si no, crear
                    if (animalLocal.id != null && animalLocal.id > 0) {
                        Log.d("GanadoRepository", "⬆️ Actualizando animal en servidor: ${animalLocal.identificacion} (serverId: ${animalLocal.id}, localId: ${animalLocal.localId})")

                        val response = api.actualizarAnimal(animalLocal.id, request)

                        if (response.success) {
                            // Marcar como sincronizado manteniendo el localId
                            val entidadActualizada = animalLocal.copy(sincronizado = true)
                            animalDao.insertarAnimal(entidadActualizada)
                            Log.d("GanadoRepository", "✅ Animal ${animalLocal.identificacion} actualizado en servidor")
                        } else {
                            Log.e("GanadoRepository", "❌ Error del servidor al actualizar ${animalLocal.identificacion}: ${response.message}")
                        }
                    } else {
                        Log.d("GanadoRepository", "⬆️ Creando nuevo animal en servidor: ${animalLocal.identificacion}")
                        val response = api.registrarAnimal(request)

                        if (response.success) {
                            val animalServidor = response.data
                            // Actualizar con el ID del servidor y marcar como sincronizado
                            val entidadActualizada = animalLocal.copy(
                                id = animalServidor.id,
                                sincronizado = true
                            )
                            animalDao.insertarAnimal(entidadActualizada)
                            Log.d("GanadoRepository", "✅ Animal ${animalLocal.identificacion} creado en servidor con ID ${animalServidor.id}")
                        } else {
                            Log.e("GanadoRepository", "❌ Error del servidor al crear ${animalLocal.identificacion}: ${response.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GanadoRepository", "❌ Excepción sincronizando animal ${animalLocal.identificacion}", e)
                }
            }

            Log.d("GanadoRepository", "=== FIN SINCRONIZACIÓN PENDIENTES ===")
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
        Log.d("GanadoRepository", "=== GET ANIMAL BY LOCAL ID ===")
        Log.d("GanadoRepository", "LocalId solicitado: $localId")

        val local = animalDao.getAnimalByLocalId(localId)

        if (local != null) {
            Log.d("GanadoRepository", "Animal encontrado en BD local:")
            Log.d("GanadoRepository", "  - LocalId: ${local.localId}")
            Log.d("GanadoRepository", "  - ServerId: ${local.id}")
            Log.d("GanadoRepository", "  - Identificación: ${local.identificacion}")
            Log.d("GanadoRepository", "  - Sincronizado: ${local.sincronizado}")

            // 🔧 CORRECCIÓN: Si NO está sincronizado, NO refrescar desde servidor
            // porque tiene cambios pendientes que se perderían
            if (!local.sincronizado) {
                Log.d("GanadoRepository", "⚠️ Animal tiene cambios pendientes, retornando versión local")
                return@withContext Result.success(local.toModel())
            }

            // Si ya está marcado como sincronizado localmente, intentamos refrescar datos
            try {
                if (local.id != null && local.id > 0) {
                    Log.d("GanadoRepository", "Intentando refrescar desde servidor con serverId=${local.id}")
                    val response = api.getAnimalById(local.id)
                    if (response.success) {
                        val animalServidor = response.data
                        Log.d("GanadoRepository", "Datos recibidos del servidor, actualizando BD local")

                        // IMPORTANTE: Guardar con el localId correcto
                        animalDao.insertarAnimal(
                            animalServidor.toEntity(sincronizado = true, localId = local.localId)
                        )

                        // Obtener el animal recién guardado de la BD
                        val animalActualizado = animalDao.getAnimalByLocalId(localId)
                        if (animalActualizado != null) {
                            Log.d("GanadoRepository", "✅ Retornando animal actualizado con localId=${animalActualizado.localId}")
                            return@withContext Result.success(animalActualizado.toModel())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GanadoRepository", "Error refrescando desde servidor, usando local", e)
            }

            // Si la API falla o el animal no tiene ID de servidor aún, devolvemos el local
            Log.d("GanadoRepository", "✅ Retornando animal local con localId=${local.localId}")
            Result.success(local.toModel())
        } else {
            Log.e("GanadoRepository", "❌ Animal no encontrado en BD local con localId=$localId")
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
        Log.d("GanadoRepository", "=== ACTUALIZAR ANIMAL BY LOCAL ID ===")
        Log.d("GanadoRepository", "LocalId: $localId")

        val animalExistente = animalDao.getAnimalByLocalId(localId)

        if (animalExistente == null) {
            Log.e("GanadoRepository", "❌ Animal no encontrado en BD local con localId: $localId")
            return@withContext Result.failure(Exception("Animal no encontrado"))
        }

        Log.d("GanadoRepository", "Animal encontrado en BD local:")
        Log.d("GanadoRepository", "  - LocalId: ${animalExistente.localId}")
        Log.d("GanadoRepository", "  - ServerId: ${animalExistente.id}")
        Log.d("GanadoRepository", "  - Identificación: ${animalExistente.identificacion}")
        Log.d("GanadoRepository", "  - Sincronizado: ${animalExistente.sincronizado}")

        try {
            if (animalExistente.id != null && animalExistente.id > 0) {
                Log.d("GanadoRepository", "Animal tiene ID servidor (${animalExistente.id}), actualizando en API...")

                val response = api.actualizarAnimal(animalExistente.id, animal)

                if (response.success) {
                    Log.d("GanadoRepository", "✅ Actualización en API exitosa")
                    val animalActualizado = response.data

                    val entidadParaGuardar = animalActualizado.toEntity(
                        sincronizado = true,
                        localId = localId
                    )

                    animalDao.insertarAnimal(entidadParaGuardar)
                    Log.d("GanadoRepository", "✅ Animal actualizado en BD local")

                    sincronizarKPIs()

                    // Leer de nuevo desde Room para obtener el objeto con localId correcto
                    val animalDesdeBD = animalDao.getAnimalByLocalId(localId)
                    if (animalDesdeBD != null) {
                        Log.d("GanadoRepository", "✅ Retornando animal desde BD con localId=${animalDesdeBD.localId}")
                        return@withContext Result.success(animalDesdeBD.toModel())
                    }

                    return@withContext Result.success(animalActualizado)
                } else {
                    Log.e("GanadoRepository", "❌ API retornó success=false: ${response.message}")
                    return@withContext Result.failure(Exception(response.message ?: "Error en servidor"))
                }
            } else {
                // Animal sin ID de servidor - guardarlo localmente y marcarlo para sincronizar
                Log.d("GanadoRepository", "⚠️ Animal SIN ID servidor, guardando solo localmente")

                val entityOffline = animal.toEntity(sincronizado = false, localId = localId)
                    .copy(id = null)

                animalDao.insertarAnimal(entityOffline)
                Log.d("GanadoRepository", "✅ Animal guardado localmente (pendiente de sincronizar)")

                context?.let {
                    programarSincronizacion(it)
                    Log.d("GanadoRepository", "📅 Sincronización programada")
                }

                return@withContext Result.success(entityOffline.toModel())
            }
        } catch (e: Exception) {
            Log.e("GanadoRepository", "❌ Excepción al actualizar", e)
            Log.d("GanadoRepository", "Guardando cambios localmente (modo offline)")

            val entityOffline = animal.toEntity(sincronizado = false, localId = localId)
                .copy(id = animalExistente.id)

            animalDao.insertarAnimal(entityOffline)

            context?.let {
                programarSincronizacion(it)
            }

            return@withContext Result.success(entityOffline.toModel())
        }
    }

    suspend fun actualizarAnimal(id: Int, animal: AnimalRequest, context: Context? = null): Result<Animal> =
        actualizarAnimalByLocalId(id, animal, context)
}