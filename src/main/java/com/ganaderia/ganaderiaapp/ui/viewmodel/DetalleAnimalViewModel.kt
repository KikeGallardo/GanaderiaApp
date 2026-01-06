package com.ganaderia.ganaderiaapp.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganaderia.ganaderiaapp.data.model.*
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DetalleAnimalViewModel : ViewModel() {
    private val repository = GanadoRepository()

    private val _animal = MutableStateFlow<Animal?>(null)
    val animal: StateFlow<Animal?> = _animal

    private val _vacunas = MutableStateFlow<List<Vacuna>>(emptyList())
    val vacunas: StateFlow<List<Vacuna>> = _vacunas

    // Usaremos StateFlow para el catálogo para mantener consistencia con el resto del VM
    private val _catalogoVacunas = MutableStateFlow<List<String>>(emptyList())
    val catalogoVacunas: StateFlow<List<String>> = _catalogoVacunas

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun cargarAnimal(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getAnimalById(id)
                .onSuccess {
                    _animal.value = it
                    cargarVacunas(id)
                    cargarCatalogo() // Cargamos el catálogo al entrar al detalle
                }
                .onFailure {
                    _error.value = it.message
                    _isLoading.value = false
                }
        }
    }

    private fun cargarVacunas(animalId: Int) {
        viewModelScope.launch {
            repository.getVacunas(animalId)
                .onSuccess { _vacunas.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    // Dentro de DetalleAnimalViewModel.kt
    // Corregido para image_0a8ff1.png
    fun cargarCatalogo() {
        viewModelScope.launch {
            repository.obtenerCatalogoVacunas()
                .onSuccess { lista: List<String> -> // Especificamos tipo explícito
                    _catalogoVacunas.value = lista
                }
                .onFailure { e ->
                    _error.value = "Error al cargar catálogo: ${e.message}"
                }
        }
    }

    // DEJA SOLO UNA VERSIÓN DE ESTA FUNCIÓN
    fun agregarAlCatalogo(nombre: String) {
        viewModelScope.launch {
            repository.guardarEnCatalogo(nombre)
                .onSuccess {
                    cargarCatalogo()
                }
                .onFailure { e ->
                    _error.value = "Error: ${e.message}"
                }
        }
    }

    fun registrarVacuna(vacuna: VacunaRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.registrarVacuna(vacuna)
                .onSuccess {
                    cargarVacunas(vacuna.animal_id)
                    onSuccess()
                }
                .onFailure { _error.value = "Error al registrar: ${it.message}" }
        }
    }
}