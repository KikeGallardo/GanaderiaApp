package com.ganaderia.ganaderiaapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganaderia.ganaderiaapp.data.model.Animal
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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        cargarAnimales()
    }

    fun cargarAnimales() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getAnimalesSinFiltros()
                .onSuccess { lista ->
                    _animales.value = lista
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Error al cargar los animales"
                }

            _isLoading.value = false
        }
    }

    fun refrescar() {
        cargarAnimales()
    }
}