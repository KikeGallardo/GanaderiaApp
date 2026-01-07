package com.ganaderia.ganaderiaapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ganaderia.ganaderiaapp.data.local.GanadoDatabase
import com.ganaderia.ganaderiaapp.data.network.RetrofitClient
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository

class GanadoViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val database = GanadoDatabase.getDatabase(context)
        val repository = GanadoRepository(
            api = RetrofitClient.instance,
            animalDao = database.animalDao(),
            vacunaDao = database.vacunaDao(),
            kpiDao = database.kpiDao()
        )

        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(repository) as T
            }
            modelClass.isAssignableFrom(DetalleAnimalViewModel::class.java) -> {
                DetalleAnimalViewModel(repository) as T
            }
            modelClass.isAssignableFrom(FormularioAnimalViewModel::class.java) -> {
                // CORRECCIÓN: Pasar contexto al FormularioViewModel
                FormularioAnimalViewModel(repository, context) as T
            }
            modelClass.isAssignableFrom(InventarioViewModel::class.java) -> {
                InventarioViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("ViewModel no reconocido: ${modelClass.name}")
        }
    }
}