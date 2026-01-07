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
import retrofit2.Response

class GanadoRepository(
    private val animalDao: AnimalDao,
    private val vacunaDao: VacunaDao,
    private val kpiDao: KpiDao,
    private val api: GanadoApiService
) {

    // 1. DASHBOARD: Flujo híbrido (Local primero, luego remoto)
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
            // Error de red: El flow termina o se queda con lo local
        }
    }

    // 2. DASHBOARD: Solo local (Para observar cambios)
    fun getKPIsLocales(): Flow<KPIs?> {
        return kpiDao.getKPIs().map { it?.toDomain() }
    }

    // 3. DASHBOARD: Forzar actualización de red
    suspend fun sincronizarKPIs() {
        try {
            val remoto = api.getKPIs()
            kpiDao.insertKPIs(remoto.toEntity())
        } catch (e: Exception) {
            // Falló el modo online, no hacemos nada
        }
    }

    // 4. INVENTARIO: Lógica Offline-First
    suspend fun getAnimales(): Result<List<Animal>> = withContext(Dispatchers.IO) {
        try {
            // 1. Intentar descargar la lista fresca del servidor
            val response = api.getAnimales()
            val animalesApi = response.data

            // 2. Sincronizar los datos de la API con Room
            // Opcional: Podrías marcar cuáles vinieron de la API para borrar los que ya no están
            animalesApi.forEach { animalApi ->
                // Buscamos si ya existe el animal por su "identificacion" (arete)
                val localExistente = animalDao.getAnimalByIdentificacion(animalApi.identificacion)

                // Mapeamos a entidad usando el localId encontrado (si existe)
                // Esto es lo que evita que se dupliquen las tarjetas
                val entidad = animalApi.toEntity(
                    sincronizado = true,
                    localId = localExistente?.localId ?: 0
                )

                // Insertamos (si el localId coincide, Room reemplaza los datos)
                animalDao.insertarAnimal(entidad)
            }

        } catch (e: Exception) {
            // Log del error para depuración, pero no bloqueamos al usuario
            e.printStackTrace()
        }

        // 3. LA ÚNICA FUENTE DE VERDAD
        // Leemos de Room. Aquí aparecerán:
        // - Los que acabamos de bajar/actualizar (nube verde)
        // - Los que creamos offline y aún no se suben (nube tachada)
        val listaDesdeBD = animalDao.getAllAnimales().map { it.toModel() }

        Result.success(listaDesdeBD)
    }

    suspend fun forceSync() = withContext(Dispatchers.IO) {
        try {
            val response = api.getAnimales()
            val listaAnimales: List<Animal> = response.data ?: emptyList()

            if (listaAnimales.isNotEmpty()) {
                listaAnimales.forEach { animalApi ->
                    // IMPORTANTE: Buscar si ya existe localmente para no perder el localId
                    val localExistente = animalDao.getAnimalByIdentificacion(animalApi.identificacion)

                    val entity = animalApi.toEntity(
                        sincronizado = true,
                        localId = localExistente?.localId ?: 0 // Mantenemos el ID local si existe
                    )
                    animalDao.insertarAnimal(entity)
                }
            }
            // Forzamos también la actualización de KPIs para el Dashboard
            sincronizarKPIs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun registrarAnimal(animal: AnimalRequest): Result<Animal> = withContext(Dispatchers.IO) {
        try {
            val response = api.registrarAnimal(animal)
            val nuevoAnimal = response.data
            animalDao.insertarAnimal(nuevoAnimal.toEntity(sincronizado = true))
            Result.success(nuevoAnimal)
        } catch (e: Exception) {
            // Si falla internet al registrar, lo guardamos localmente como no sincronizado
            val entity = animal.toEntity(sincronizado = false)
            animalDao.insertarAnimal(entity)
            Result.failure(e)
        }
    }

    suspend fun marcarComoSincronizado(id: Int) = withContext(Dispatchers.IO) {
        val entity = animalDao.getAnimalById(id)
        entity?.let {
            animalDao.insertarAnimal(it.copy(sincronizado = true))
        }
    }

    // 6. OTRAS FUNCIONES (Catalogo, Vacunas, etc.)
    // Cambia el parámetro de Int a Int (usaremos el localId)
    suspend fun getAnimalById(localId: Int): Result<Animal> = withContext(Dispatchers.IO) {
        // 1. Buscamos siempre primero en Room usando el localId
        val local = animalDao.getAnimalByLocalId(localId)

        if (local != null) {
            // 2. Si existe localmente y NO está sincronizado, devolvemos el local de una vez
            if (!local.sincronizado) {
                return@withContext Result.success(local.toModel())
            }

            // 3. Si está sincronizado, intentamos refrescar desde la API usando el ID del servidor
            try {
                val response = api.getAnimalById(local.id!!)
                val animalServidor = response.data
                // Actualizamos el registro local con lo que diga el servidor
                animalDao.insertarAnimal(animalServidor.toEntity(sincronizado = true, localId = local.localId))
                Result.success(animalServidor)
            } catch (e: Exception) {
                // Si falla la red, devolvemos lo que ya teníamos en Room
                Result.success(local.toModel())
            }
        } else {
            Result.failure(Exception("Animal no encontrado"))
        }
    }

    // 1. Esta función solo se comunica con el Servidor (Usada por el Worker)
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

    // 2. Esta función actualiza Room de forma segura (Usada por el Worker)
    suspend fun actualizarAnimalLocal(entidad: AnimalEntity) = withContext(Dispatchers.IO) {
        // Como tu entidad tiene @PrimaryKey(autoGenerate = true) en localId,
        // Room buscará ese localId y reemplazará toda la fila con la nueva info (id del servidor y sincronizado = true)
        animalDao.insertarAnimal(entidad)
    }

    // 3. Función auxiliar que necesita el Worker para obtener los pendientes
    suspend fun getAnimalesNoSincronizados(): List<AnimalEntity> = withContext(Dispatchers.IO) {
        // Asegúrate de tener esta consulta en tu AnimalDao
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

    // --- 7. VACUNAS Y CATÁLOGO (Faltaban estas funciones) ---

    suspend fun getVacunas(animalId: Int): Result<List<Vacuna>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getVacunas(animalId)
            Result.success(response.data)
        } catch (e: Exception) {
            // Aquí podrías opcionalmente cargar vacunas locales si tuvieras un VacunaDao.getAll(animalId)
            Result.failure(e)
        }
    }

    suspend fun obtenerCatalogoVacunas(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCatalogo()
            Result.success(response.data ?: emptyList())
        } catch (e: Exception) {
            // Si falla la red, el ViewModel usará la lista por defecto que ya programaste
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

    suspend fun eliminarAnimal(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Intentar borrar en el servidor
            val response = api.eliminarAnimal(id)
            // 2. Si tiene éxito o falla, borramos localmente para que desaparezca de la vista
            animalDao.eliminarAnimalPorId(id)
            Result.success(Unit)
        } catch (e: Exception) {
            // MODO OFFLINE: Borramos de la DB local para que el usuario vea que "se fue"
            // Nota: En una app profesional, marcarías un campo 'isDeleted' para sincronizar luego,
            // pero para arreglar el botón ahora, lo borramos localmente:
            animalDao.eliminarAnimalPorId(id)
            Result.success(Unit)
        }
    }

    suspend fun registrarVacuna(request: VacunaRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.registrarVacuna(request)
            // Guardamos copia local marcada como sincronizada si el servidor respondió bien
            vacunaDao.insertVacuna(request.toEntity(sincronizado = response.success))
            Result.success(Unit)
        } catch (e: Exception) {
            // OFFLINE: Guardamos localmente para subir después
            vacunaDao.insertVacuna(request.toEntity(sincronizado = false))
            Result.success(Unit) // Retornamos éxito para que la UI cierre el formulario
        }
    }

    suspend fun actualizarAnimal(id: Int, animal: AnimalRequest): Result<Animal> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Intentar actualizar en el servidor
                val response = api.actualizarAnimal(id, animal)
                val animalActualizado = response.data

                // 2. Guardar en la base de datos local y marcar como sincronizado
                animalDao.insertarAnimal(animalActualizado.toEntity(sincronizado = true))

                Result.success(animalActualizado)
            } catch (e: Exception) {
                // 3. MODO OFFLINE: Si falla internet, actualizamos lo local
                // pero marcamos sincronizado = false para que el Worker lo suba después
                val entityOffline = animal.toEntity(sincronizado = false).copy(id = id)
                animalDao.insertarAnimal(entityOffline)

                // Convertimos la entidad a modelo para que la UI se actualice
                Result.success(entityOffline.toModel())
            }
        }
}