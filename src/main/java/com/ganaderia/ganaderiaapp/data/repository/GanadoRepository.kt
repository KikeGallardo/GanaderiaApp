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
            // 🔧 CORRECCIÓN 1: Sincronizar pendientes PRIMERO
            Log.d("GanadoRepository", "Sincronizando cambios pendientes antes de traer datos del servidor...")
            sincronizarAnimalesPendientes()

            val response = api.getAnimales()
            val animalesApi = response.data

            animalesApi.forEach { animalApi ->
                val localExistente = animalDao.getAnimalByServerId(animalApi.id)

                // REGLA DE ORO: Si ya existe y sincronizado es false (0), NO TOCAR
                if (localExistente != null && !localExistente.sincronizado) {
                    Log.d("GanadoRepository", "⚠️ Saltando ${animalApi.identificacion}: cambios locales pendientes")
                    return@forEach // Salta al siguiente y no ejecuta el insertarAnimal
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
            // 🔧 CORRECCIÓN: Siempre sincronizar pendientes PRIMERO
            Log.d("GanadoRepository", "=== FORCE SYNC: Sincronizando pendientes primero ===")
            sincronizarAnimalesPendientes()

            Log.d("GanadoRepository", "=== FORCE SYNC: Trayendo datos del servidor ===")
            val response = api.getAnimales()
            val listaAnimales = response.data

            // Busca este bloque dentro de forceSync()
            // DENTRO DE suspend fun forceSync()
            listaAnimales.forEach { animalApi ->
                val localExistente = animalDao.getAnimalByServerId(animalApi.id)

                // Protección contra sobreescritura de pendientes
                if (localExistente != null && !localExistente.sincronizado) {
                    Log.d("GanadoRepository", "⚠️ Conservando cambios pendientes de ${animalApi.identificacion}")
                    return@forEach
                }

                val entity = animalApi.toEntity(
                    sincronizado = true,
                    localId = localExistente?.localId ?: 0
                )
                animalDao.insertarAnimal(entity)
            }

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
            // 🔧 DEBUG: Ver TODOS los animales en BD
            try {
                val todosDebug = animalDao.getAllAnimalesDebug()
                Log.d("GanadoRepository", "=== DEBUG: TODOS LOS ANIMALES EN BD ===")
                todosDebug.forEach { animal ->
                    Log.d("GanadoRepository", "  LocalId: ${animal.localId}, ServerId: ${animal.id}, " +
                            "Identificacion: ${animal.identificacion}, Sincronizado: ${animal.sincronizado}, Activo: ${animal.activo}")
                }
            } catch (e: Exception) {
                Log.e("GanadoRepository", "Error en debug", e)
            }

            val noSincronizados = animalDao.getNoSincronizados()
            Log.d("GanadoRepository", "=== SINCRONIZANDO ${noSincronizados.size} ANIMALES PENDIENTES ===")

            noSincronizados.forEach { animalLocal ->
                try {
                    val request = animalLocal.toRequest()

                    if (animalLocal.id != null && animalLocal.id > 0) {
                        // 🔧 ACTUALIZAR en servidor
                        Log.d("GanadoRepository", "⬆️ Actualizando animal en servidor: ${animalLocal.identificacion} (serverId: ${animalLocal.id})")

                        val response = api.actualizarAnimal(animalLocal.id, request)

                        if (response.success) {
                            // Usa true en lugar de 1 si tu Entity define sincronizado como Boolean
                            val entidadActualizada = animalLocal.copy(sincronizado = true)
                            animalDao.insertarAnimal(entidadActualizada)
                            Log.d("GanadoRepository", "✅ Animal ${animalLocal.identificacion} actualizado en servidor")
                        } else {
                            Log.e("GanadoRepository", "❌ Error del servidor: ${response.message}")
                        }
                    } else {
                        // 🔧 CREAR nuevo en servidor
                        Log.d("GanadoRepository", "⬆️ Creando nuevo animal en servidor: ${animalLocal.identificacion}")
                        val response = api.registrarAnimal(request)

                        if (response.success) {
                            val animalServidor = response.data
                            // Actualizar con ID del servidor
                            val entidadActualizada = animalLocal.copy(
                                id = animalServidor.id,
                                sincronizado = true // Usa true
                            )
                            animalDao.insertarAnimal(entidadActualizada)
                            Log.d("GanadoRepository", "✅ Animal ${animalLocal.identificacion} creado con ID ${animalServidor.id}")
                        } else {
                            Log.e("GanadoRepository", "❌ Error del servidor: ${response.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GanadoRepository", "❌ Excepción sincronizando ${animalLocal.identificacion}", e)
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

    suspend fun refrescarNombresPadres(localId: Int, serverId: Int) = withContext(Dispatchers.IO) {
        try {
            val response = api.getAnimalById(serverId)
            if (response.success) {
                val animalRed = response.data
                // Solo guardamos los textos de identificación obtenidos del JOIN del servidor
                animalDao.actualizarNombresPadres(
                    localId,
                    animalRed.madre_identificacion,
                    animalRed.padre_identificacion
                )
            }
        } catch (e: Exception) {
            Log.e("GanadoRepository", "Error refrescando nombres: ${e.message}")
        }
    }

    suspend fun getAnimalByLocalId(localId: Int): Result<Animal> = withContext(Dispatchers.IO) {
        Log.d("GanadoRepository", "=== GET ANIMAL BY LOCAL ID ===")
        Log.d("GanadoRepository", "LocalId solicitado: $localId")

        val local = animalDao.getAnimalByLocalId(localId)

        if (local != null) {
            Log.d("GanadoRepository", "Animal encontrado:")
            Log.d("GanadoRepository", "  - LocalId: ${local.localId}")
            Log.d("GanadoRepository", "  - ServerId: ${local.id}")
            Log.d("GanadoRepository", "  - Sincronizado: ${local.sincronizado}")

            // 🔧 CRÍTICO: SIEMPRE retornar datos locales sin intentar refrescar desde servidor
            // La sincronización desde servidor solo debe ocurrir en getAnimales() y forceSync()
            // que ya tienen la lógica de no sobrescribir cambios pendientes
            Log.d("GanadoRepository", "✅ Retornando versión LOCAL (sin refrescar desde servidor)")
            Result.success(local.toModel())
        } else {
            Log.e("GanadoRepository", "❌ Animal no encontrado con localId=$localId")
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

    // Simplemente hace la llamada a la API sin lógica local, para uso del Worker
    suspend fun actualizarAnimalApiDirecto(id: Int, request: AnimalRequest): Result<Animal> = withContext(Dispatchers.IO) {
        try {
            val response = api.actualizarAnimal(id, request)
            if (response.success) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
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

            Log.d("GanadoRepository", "Eliminando animal - localId: ${animal.localId}, serverId: ${animal.id}")

            if (animal.id != null && animal.id > 0 && animal.sincronizado) {
                try {
                    Log.d("GanadoRepository", "Eliminando del servidor con ID: ${animal.id}")
                    val response = api.eliminarAnimal(animal.id)
                    Log.d("GanadoRepository", "Animal eliminado del servidor exitosamente")
                } catch (e: Exception) {
                    Log.w("GanadoRepository", "Error eliminando del servidor: ${e.message}")
                }
            } else {
                Log.d("GanadoRepository", "Animal no sincronizado, solo se eliminará localmente")
            }

            animalDao.eliminarPorLocalId(localId)
            Log.d("GanadoRepository", "Animal marcado como inactivo localmente")

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
        Log.d("GanadoRepository", "=== EDITANDO ANIMAL: $localId ===")

        val animalExistente = animalDao.getAnimalByLocalId(localId)
        if (animalExistente == null) return@withContext Result.failure(Exception("Animal no encontrado"))

        // 1. GUARDADO INICIAL (Modo Offline/Pendiente)
        // Forzamos sincronizado = false (0) para que el SyncWorker lo detecte
        val entityOffline = animal.toEntity(sincronizado = false, localId = localId)
            .copy(id = animalExistente.id, sincronizado = false) // Aseguramos false explícitamente

        animalDao.insertarAnimal(entityOffline)
        Log.d("GanadoRepository", "💾 Guardado localmente como PENDIENTE (Sincronizado: 0)")

        // Programar Worker para cuando vuelva la conexión
        context?.let { programarSincronizacion(it) }

        try {
            if (animalExistente.id != null && animalExistente.id > 0) {
                Log.d("GanadoRepository", "⬆️ Intentando subir cambios al servidor (ID: ${animalExistente.id})...")

                val response = api.actualizarAnimal(animalExistente.id, animal)

                if (response.success) {
                    // 2. ÉXITO EN RED: Ahora sí marcamos como sincronizado = true (1)
                    val animalActualizado = response.data
                    val entidadSincronizada = animalActualizado.toEntity(
                        sincronizado = true,
                        localId = localId
                    )

                    animalDao.insertarAnimal(entidadSincronizada)
                    Log.d("GanadoRepository", "✅ Sincronización exitosa. (Sincronizado: 1)")

                    sincronizarKPIs()
                    return@withContext Result.success(animalActualizado)
                } else {
                    Log.e("GanadoRepository", "⚠️ El servidor rechazó el cambio: ${response.message}")
                }
            }
            // Si no tiene ID de servidor, retorna la versión offline (permanece en 0)
            return@withContext Result.success(entityOffline.toModel())

        } catch (e: Exception) {
            // 3. ERROR DE RED: Los datos ya están seguros en la BD local con sincronizado = 0
            Log.e("GanadoRepository", "📶 Error de red detectado. El animal quedará pendiente de subida.")
            return@withContext Result.success(entityOffline.toModel())
        }
    }

    suspend fun actualizarAnimal(id: Int, animal: AnimalRequest, context: Context? = null): Result<Animal> =
        actualizarAnimalByLocalId(id, animal, context)
}