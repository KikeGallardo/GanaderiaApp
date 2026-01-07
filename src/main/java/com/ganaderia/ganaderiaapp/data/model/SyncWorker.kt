package com.ganaderia.ganaderiaapp.data.model

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository
import com.ganaderia.ganaderiaapp.data.local.GanadoDatabase
import com.ganaderia.ganaderiaapp.data.network.RetrofitClient

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "=== INICIANDO SINCRONIZACIÓN ===")

        val database = GanadoDatabase.getDatabase(applicationContext)
        val repository = GanadoRepository(
            api = RetrofitClient.instance,
            animalDao = database.animalDao(),
            vacunaDao = database.vacunaDao(),
            kpiDao = database.kpiDao()
        )

        return try {
            val noSincronizados = repository.getAnimalesNoSincronizados()
            Log.d("SyncWorker", "Encontrados ${noSincronizados.size} animales pendientes de sincronización")

            var exitosos = 0
            var fallidos = 0

            noSincronizados.forEach { animalLocal ->
                try {
                    val request = animalLocal.toRequest()

                    // Si tiene ID del servidor, actualizar; si no, crear nuevo
                    if (animalLocal.id != null && animalLocal.id > 0) {
                        Log.d("SyncWorker", "Actualizando animal: ${animalLocal.identificacion} (ID: ${animalLocal.id})")
                        val resultado = repository.actualizarAnimalLocal(
                            animalLocal.copy(sincronizado = true)
                        )
                        exitosos++
                    } else {
                        Log.d("SyncWorker", "Creando nuevo animal: ${animalLocal.identificacion}")
                        val resultado = repository.registrarAnimalApiDirecto(request)

                        resultado.onSuccess { animalServidor ->
                            // Actualizar el registro local con el ID del servidor
                            val entidadActualizada = animalLocal.copy(
                                id = animalServidor.id,
                                sincronizado = true
                            )
                            repository.actualizarAnimalLocal(entidadActualizada)
                            Log.d("SyncWorker", "✓ Animal ${animalLocal.identificacion} sincronizado con ID ${animalServidor.id}")
                            exitosos++
                        }.onFailure { error ->
                            Log.e("SyncWorker", "✗ Error sincronizando ${animalLocal.identificacion}: ${error.message}")
                            fallidos++
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SyncWorker", "✗ Excepción sincronizando ${animalLocal.identificacion}", e)
                    fallidos++
                }
            }

            // Sincronizar KPIs al final
            repository.sincronizarKPIs()

            Log.d("SyncWorker", "=== SINCRONIZACIÓN COMPLETADA: $exitosos exitosos, $fallidos fallidos ===")

            if (fallidos > 0 && exitosos == 0) {
                Result.retry() // Reintentar si todos fallaron
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error general en sincronización", e)
            Result.retry()
        }
    }
}