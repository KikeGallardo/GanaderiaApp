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
                        Log.d("SyncWorker", "Actualizando animal ${animalLocal.identificacion} (serverId: ${animalLocal.id})")

                        val resultado = repository.actualizarAnimalApiDirecto(animalLocal.id, request)

                        if (resultado.isSuccess) {
                            val actualizado = animalLocal.copy(sincronizado = true)
                            repository.actualizarAnimalLocal(actualizado)
                            exitosos++
                            Log.d("SyncWorker", "✅ Animal ${animalLocal.identificacion} actualizado exitosamente")
                        } else {
                            fallidos++
                            Log.e("SyncWorker", "❌ Error actualizando ${animalLocal.identificacion}")
                        }
                    } else {
                        // CASO: CREACIÓN NUEVA PENDIENTE
                        Log.d("SyncWorker", "Creando nuevo animal ${animalLocal.identificacion}")

                        val resultado = repository.registrarAnimalApiDirecto(request)

                        // 🔧 CORRECCIÓN AQUÍ: Manejo seguro de nulabilidad del ID
                        if (resultado.isSuccess) {
                            val animalServidor = resultado.getOrNull()

                            // Si el servidor solo mandó el ID y no el objeto completo
                            val serverId = animalServidor?.id ?: 0

                            if (serverId > 0) {
                                val actualizado = animalLocal.copy(
                                    id = serverId,
                                    sincronizado = true
                                )
                                repository.actualizarAnimalLocal(actualizado)
                                exitosos++
                                Log.d("SyncWorker", "✅ Animal ${animalLocal.identificacion} creado con ID $serverId")
                            } else {
                                fallidos++
                                Log.e("SyncWorker", "❌ El servidor no devolvió un ID válido")
                            }
                        } else {
                            fallidos++
                            Log.e("SyncWorker", "❌ Error creando ${animalLocal.identificacion}")
                        }
                    }
                } catch (e: Exception) {
                    fallidos++
                    Log.e("SyncWorker", "❌ Excepción procesando ${animalLocal.identificacion}", e)
                }
            }

            try {
                repository.sincronizarKPIs()
                Log.d("SyncWorker", "✅ KPIs sincronizados")
            } catch (e: Exception) {
                Log.e("SyncWorker", "⚠️ Error sincronizando KPIs", e)
            }

            Log.d("SyncWorker", "=== SINCRONIZACIÓN COMPLETADA: $exitosos exitosos, $fallidos fallidos ===")

            when {
                fallidos > 0 && exitosos == 0 -> Result.retry()
                else -> Result.success()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "❌ Error general en sincronización", e)
            Result.retry()
        }
    }
}