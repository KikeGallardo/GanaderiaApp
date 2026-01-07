// ============================================
// InventarioViewModel.kt con Auto-Refresh
// ============================================
package com.ganaderia.ganaderiaapp.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganaderia.ganaderiaapp.data.model.Animal
import com.ganaderia.ganaderiaapp.data.model.toRequest
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class InventarioViewModel(private val repository: GanadoRepository) : ViewModel() {

    private val _animales = MutableStateFlow<List<Animal>>(emptyList())
    val animales: StateFlow<List<Animal>> = _animales.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        cargarAnimales()
    }


    // 🔧 NUEVO: Auto-refresh cada 30 segundos
    /*

    private fun iniciarAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            delay(5000) // Esperar 5s después de carga inicial

            while (isActive) {
                try {
                    Log.d("InventarioViewModel", "🔄 Auto-refresh ejecutándose...")
                    sincronizarEnSegundoPlano()
                    delay(30000) // 30 segundos
                } catch (e: Exception) {
                    Log.e("InventarioViewModel", "Error en auto-refresh", e)
                    delay(60000) // Si hay error, esperar 1 minuto
                }
            }
        }
    }*/

private suspend fun sincronizarEnSegundoPlano() {
    try {
        val noSincronizados = repository.getAnimalesNoSincronizados()

        if (noSincronizados.isNotEmpty()) {
            Log.d("InventarioViewModel", "⏳ Sincronizando ${noSincronizados.size} animales pendientes...")

            noSincronizados.forEach { animalLocal ->
                try {
                    val request = animalLocal.toRequest()

                    if (animalLocal.id != null && animalLocal.id > 0) {
                        val entidadActualizada = animalLocal.copy(sincronizado = true)
                        repository.actualizarAnimalLocal(entidadActualizada)
                    } else {
                        val resultado = repository.registrarAnimalApiDirecto(request)
                        resultado.onSuccess { animalServidor ->
                            val entidadActualizada = animalLocal.copy(
                                id = animalServidor.id,
                                sincronizado = true
                            )
                            repository.actualizarAnimalLocal(entidadActualizada)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("InventarioViewModel", "Error sincronizando ${animalLocal.identificacion}", e)
                }
            }
        }

        // Recargar datos
        repository.getAnimalesSinFiltros()
            .onSuccess { lista ->
                _animales.value = lista
            }
    } catch (e: Exception) {
        Log.e("InventarioViewModel", "Error en sincronización de fondo", e)
    }
}

fun cargarAnimales() {
    viewModelScope.launch {
        _isLoading.value = true
        _error.value = null

        Log.d("InventarioViewModel", "Cargando animales")

        repository.getAnimalesSinFiltros()
            .onSuccess { lista ->
                _animales.value = lista
                val sincronizados = lista.count { it.sincronizado }
                val noSincronizados = lista.count { it.sincronizado }
                Log.d("InventarioViewModel", "Cargados ${lista.size} animales ($sincronizados sincronizados, $noSincronizados pendientes)")
            }
            .onFailure { e ->
                _error.value = e.message ?: "Error al cargar los animales"
                Log.e("InventarioViewModel", "Error cargando animales", e)
            }

        _isLoading.value = false
    }
}

// Mantener función manual por si el usuario quiere forzar
fun forzarSincronizacion(context: Context) {
    viewModelScope.launch {
        _isSyncing.value = true
        _error.value = null

        Log.d("InventarioViewModel", "Sincronización forzada manual")

        try {
            val noSincronizados = repository.getAnimalesNoSincronizados()
            Log.d("InventarioViewModel", "Encontrados ${noSincronizados.size} animales para sincronizar")

            noSincronizados.forEach { animalLocal ->
                try {
                    val request = animalLocal.toRequest()

                    if (animalLocal.id != null && animalLocal.id > 0) {
                        Log.d("InventarioViewModel", "Actualizando: ${animalLocal.identificacion}")
                        val entidadActualizada = animalLocal.copy(sincronizado = true)
                        repository.actualizarAnimalLocal(entidadActualizada)
                    } else {
                        Log.d("InventarioViewModel", "Creando: ${animalLocal.identificacion}")
                        val resultado = repository.registrarAnimalApiDirecto(request)

                        resultado.onSuccess { animalServidor ->
                            val entidadActualizada = animalLocal.copy(
                                id = animalServidor.id,
                                sincronizado = true
                            )
                            repository.actualizarAnimalLocal(entidadActualizada)
                            Log.d("InventarioViewModel", "✓ Sincronizado: ${animalLocal.identificacion}")
                        }.onFailure { error ->
                            Log.e("InventarioViewModel", "✗ Error: ${error.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("InventarioViewModel", "Error: ${animalLocal.identificacion}", e)
                }
            }

            repository.forceSync()
            cargarAnimales()

            Log.d("InventarioViewModel", "Sincronización completada")
        } catch (e: Exception) {
            _error.value = "Error en sincronización: ${e.message}"
            Log.e("InventarioViewModel", "Error en sincronización", e)
        } finally {
            _isSyncing.value = false
        }
    }
}

fun refrescar() {
    Log.d("InventarioViewModel", "Refrescando inventario")
    cargarAnimales()
}

override fun onCleared() {
    super.onCleared()
    autoRefreshJob?.cancel()
}
}