package com.ganaderia.ganaderiaapp.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganaderia.ganaderiaapp.data.model.Animal
import com.ganaderia.ganaderiaapp.data.model.toRequest
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init {
        cargarAnimales()
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
                    val noSincronizados = lista.count { !it.sincronizado }
                    Log.d("InventarioViewModel", "Cargados ${lista.size} animales ($sincronizados sincronizados, $noSincronizados pendientes)")
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Error al cargar los animales"
                    Log.e("InventarioViewModel", "Error cargando animales", e)
                }

            _isLoading.value = false
        }
    }

    fun forzarSincronizacion(context: Context) {
        viewModelScope.launch {
            _isSyncing.value = true
            _error.value = null

            Log.d("InventarioViewModel", "Iniciando sincronización forzada")

            try {
                // Primero sincronizar animales pendientes
                val noSincronizados = repository.getAnimalesNoSincronizados()
                Log.d("InventarioViewModel", "Encontrados ${noSincronizados.size} animales para sincronizar")

                noSincronizados.forEach { animalLocal ->
                    try {
                        val request = animalLocal.toRequest()

                        if (animalLocal.id != null && animalLocal.id > 0) {
                            Log.d("InventarioViewModel", "Actualizando animal en servidor: ${animalLocal.identificacion}")
                            // Ya tiene ID del servidor, actualizar
                            val entidadActualizada = animalLocal.copy(sincronizado = true)
                            repository.actualizarAnimalLocal(entidadActualizada)
                        } else {
                            Log.d("InventarioViewModel", "Creando nuevo animal en servidor: ${animalLocal.identificacion}")
                            // No tiene ID del servidor, crear nuevo
                            val resultado = repository.registrarAnimalApiDirecto(request)

                            resultado.onSuccess { animalServidor ->
                                val entidadActualizada = animalLocal.copy(
                                    id = animalServidor.id,
                                    sincronizado = true
                                )
                                repository.actualizarAnimalLocal(entidadActualizada)
                                Log.d("InventarioViewModel", "✓ Animal sincronizado: ${animalLocal.identificacion} -> ID ${animalServidor.id}")
                            }.onFailure { error ->
                                Log.e("InventarioViewModel", "✗ Error sincronizando ${animalLocal.identificacion}: ${error.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("InventarioViewModel", "Error procesando animal ${animalLocal.identificacion}", e)
                    }
                }

                // Luego hacer sync completo
                repository.forceSync()

                // Recargar lista
                cargarAnimales()

                Log.d("InventarioViewModel", "Sincronización completada")
            } catch (e: Exception) {
                _error.value = "Error en sincronización: ${e.message}"
                Log.e("InventarioViewModel", "Error en sincronización forzada", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun refrescar() {
        Log.d("InventarioViewModel", "Refrescando inventario")
        cargarAnimales()
    }
}