package com.ganaderia.ganaderiaapp.data.model

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository
import com.ganaderia.ganaderiaapp.data.local.GanadoDatabase
import com.ganaderia.ganaderiaapp.data.local.entities.AnimalEntity
import com.ganaderia.ganaderiaapp.data.network.RetrofitClient

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = GanadoDatabase.getDatabase(applicationContext)
        val repository = GanadoRepository(
            database.animalDao(),
            database.vacunaDao(),
            database.kpiDao(),
            RetrofitClient.instance
        )

        return try {
            // 1. Obtener solo los que no están en el servidor
            val noSincronizados: List<AnimalEntity> = repository.getAnimalesNoSincronizados()

            noSincronizados.forEach { animalLocal ->
                // 2. Intentar registrar en el servidor
                val request = animalLocal.toRequest()
                val resultado = repository.registrarAnimalApiDirecto(request) // Usar función que solo toque API

                resultado.onSuccess { animalServidor ->
                    // 3. ¡CLAVE!: Actualizamos el registro local usando el localId original
                    // pero ahora le ponemos el ID que nos dio el servidor y sincronizado = true
                    val entidadActualizada = animalLocal.copy(
                        id = animalServidor.id, // El ID real del servidor (ej. 45)
                        sincronizado = true
                    )

                    // Guardamos (esto reemplaza la fila vieja por el localId)
                    repository.actualizarAnimalLocal(entidadActualizada)
                }
            }
            Result.success()
        } catch (e: Exception) {
            // Si es un error de red, intentamos más tarde
            Result.retry()
        }
    }
}