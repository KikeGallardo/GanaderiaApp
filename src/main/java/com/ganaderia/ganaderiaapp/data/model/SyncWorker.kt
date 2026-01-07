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
            api = RetrofitClient.instance,
            animalDao = database.animalDao(),
            vacunaDao = database.vacunaDao(),
            kpiDao = database.kpiDao()
        )

        return try {
            val noSincronizados: List<AnimalEntity> = repository.getAnimalesNoSincronizados()

            noSincronizados.forEach { animalLocal ->
                val request = animalLocal.toRequest()
                val resultado = repository.registrarAnimalApiDirecto(request)

                resultado.onSuccess { animalServidor ->
                    val entidadActualizada = animalLocal.copy(
                        id = animalServidor.id,
                        sincronizado = true
                    )

                    repository.actualizarAnimalLocal(entidadActualizada)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}