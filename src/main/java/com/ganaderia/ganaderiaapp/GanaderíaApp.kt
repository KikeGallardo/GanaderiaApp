package com.ganaderia.ganaderiaapp

import android.app.Application
import com.ganaderia.ganaderiaapp.data.local.GanadoDatabase
import com.ganaderia.ganaderiaapp.data.network.RetrofitClient
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository

class GanaderiaApp : Application() {

    // Inicializamos la base de datos usando un delegado lazy (solo cuando se use)
    private val database by lazy { GanadoDatabase.getDatabase(this) }

    // Inicializamos el repositorio de forma global
    val repository by lazy {
        GanadoRepository(
            api = RetrofitClient.instance,
            animalDao = database.animalDao(),
            vacunaDao = database.vacunaDao(),
            kpiDao = database.kpiDao()
        )
    }
}
