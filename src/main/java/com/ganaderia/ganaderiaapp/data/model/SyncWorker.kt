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

                    if (animalLocal.id != null && animalLocal.id > 0) {
                        // CASO: EDICIÓN PENDIENTE
                        // Usamos kotlin.Result para evitar conflicto con el Result de WorkManager
                        val resultado: kotlin.Result<com.ganaderia.ganaderiaapp.data.model.Animal> =
                            repository.actualizarAnimalApiDirecto(animalLocal.id, request)

                        resultado.onSuccess {
                            repository.actualizarAnimalLocal(animalLocal.copy(sincronizado = true))
                            exitosos++
                        }.onFailure { error ->
                            fallidos++
                        }
                    } else {
                        // CASO: CREACIÓN NUEVA PENDIENTE
                        val resultado: kotlin.Result<com.ganaderia.ganaderiaapp.data.model.Animal> =
                            repository.registrarAnimalApiDirecto(request)

                        resultado.onSuccess { animalServidor ->
                            repository.actualizarAnimalLocal(animalLocal.copy(
                                id = animalServidor.id, // Aquí ya no fallará el ID
                                sincronizado = true
                            ))
                            exitosos++
                        }.onFailure {
                            fallidos++
                        }
                    }
                } catch (e: Exception) {
                    fallidos++
                }
            }

            // Sincronizar KPIs al final
            repository.sincronizarKPIs()

            Log.d("SyncWorker", "=== SINCRONIZACIÓN COMPLETADA: $exitosos exitosos, $fallidos fallidos ===")

            // Aquí SÍ usamos el Result de WorkManager (androidx.work.ListenableWorker.Result)
            return if (fallidos > 0 && exitosos == 0) {
                androidx.work.ListenableWorker.Result.retry()
            } else {
                androidx.work.ListenableWorker.Result.success()
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