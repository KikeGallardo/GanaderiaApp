package com.ganaderia.ganaderiaapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganaderia.ganaderiaapp.data.model.*
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DetalleAnimalViewModel(private val repository: GanadoRepository) : ViewModel() {
    private val _animal = MutableStateFlow<Animal?>(null)
    val animal: StateFlow<Animal?> = _animal

    private val _vacunas = MutableStateFlow<List<Vacuna>>(emptyList())
    val vacunas: StateFlow<List<Vacuna>> = _vacunas

    private val _catalogoVacunas = MutableStateFlow<List<String>>(emptyList())
    val catalogoVacunas: StateFlow<List<String>> = _catalogoVacunas

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val catalogoPorDefecto = listOf(
        "Triple Viral", "Rabia", "Carbunco", "Aftosa",
        "IBR/DVB", "Clostridiasis", "Brucelosis", "Leptospirosis"
    )

    fun cargarAnimal(localId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getAnimalByLocalId(localId)
                .onSuccess { animal ->
                    _animal.value = animal

                    if (animal.id > 0) {
                        cargarVacunas(animal.id)
                    }

                    cargarCatalogo()
                }
                .onFailure { e ->
                    if (_animal.value == null) {
                        _error.value = "Error al cargar animal: ${e.message}"
                    }
                }

            _isLoading.value = false
        }
    }

    private fun cargarVacunas(animalId: Int) {
        viewModelScope.launch {
            repository.getVacunas(animalId)
                .onSuccess {
                    _vacunas.value = it
                }
                .onFailure {
                    // Fallo silencioso
                }
        }
    }

    fun cargarCatalogo() {
        viewModelScope.launch {
            repository.obtenerCatalogoVacunas()
                .onSuccess { lista ->
                    _catalogoVacunas.value = lista.ifEmpty { catalogoPorDefecto }
                }
                .onFailure {
                    if (_catalogoVacunas.value.isEmpty()) {
                        _catalogoVacunas.value = catalogoPorDefecto
                    }
                }
        }
    }

    fun agregarAlCatalogo(nombre: String) {
        viewModelScope.launch {
            repository.guardarEnCatalogo(nombre)
                .onSuccess {
                    cargarCatalogo()
                }
        }
    }

    fun eliminarAnimal(localId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true

            repository.eliminarAnimalByLocalId(localId)
                .onSuccess {
                    _isLoading.value = false
                    // CORRECCIÓN: Cerrar pantalla inmediatamente
                    onSuccess()
                }
                .onFailure {
                    _error.value = "Error al eliminar: ${it.message}"
                    _isLoading.value = false
                }
        }
    }

    fun eliminarVacuna(vacunaId: Int, animalId: Int) {
        viewModelScope.launch {
            _isLoading.value = true

            repository.eliminarVacuna(vacunaId)
                .onSuccess {
                    cargarVacunas(animalId)
                }
                .onFailure {
                    _error.value = "No se pudo eliminar: ${it.message}"
                }

            _isLoading.value = false
        }
    }

    fun registrarVacuna(vacuna: VacunaRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.registrarVacuna(vacuna)
                .onSuccess {
                    cargarVacunas(vacuna.animal_id)
                    onSuccess()
                }
                .onFailure {
                    cargarVacunas(vacuna.animal_id)
                    onSuccess()
                }
        }
    }
}