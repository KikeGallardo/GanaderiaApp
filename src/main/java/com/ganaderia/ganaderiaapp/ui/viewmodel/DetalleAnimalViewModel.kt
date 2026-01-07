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

    fun cargarAnimal(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            // No borramos el animal actual para evitar parpadeos si ya había algo
            repository.getAnimalById(id)
                .onSuccess {
                    _animal.value = it
                    _error.value = null // Si hay éxito (red o local), quitamos el error
                    cargarVacunas(id)
                    cargarCatalogo()
                }
                .onFailure { e ->
                    // MODO HÍBRIDO: Solo mostramos la pantalla de error si NO hay datos locales
                    if (_animal.value == null) {
                        _error.value = "Sin conexión: ${e.message}"
                    }
                    // Si _animal.value NO es nulo, significa que el repositorio ya nos dio
                    // los datos locales a través del onSuccess antes de fallar la red.
                }
            _isLoading.value = false
        }
    }

    private fun cargarVacunas(animalId: Int) {
        viewModelScope.launch {
            repository.getVacunas(animalId)
                .onSuccess {
                    _vacunas.value = it
                    // No tocamos _error para no sobreescribir errores del animal
                }
                .onFailure {
                    // Fallo silencioso: si no hay red, simplemente no actualiza la lista
                }
            _isLoading.value = false
        }
    }

    fun cargarCatalogo() {
        viewModelScope.launch {
            repository.obtenerCatalogoVacunas()
                .onSuccess { lista: List<String> ->
                    _catalogoVacunas.value = lista.ifEmpty { catalogoPorDefecto }
                }
                .onFailure {
                    // MODO OFFLINE: Si falla la red, usamos la lista local sin disparar un estado de Error crítico
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
                .onFailure { e ->
                    // Aquí sí es útil avisar, pero quizás con un Toast o snackbar (vía eventos)
                    // Por ahora evitamos bloquear la pantalla principal
                }
        }
    }

    fun eliminarAnimal(id: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.eliminarAnimal(id)
                .onSuccess {
                    _isLoading.value = false
                    onSuccess() // Esto cerrará la pantalla y volverá al inventario
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
                .onFailure { t ->
                    _error.value = "No se pudo eliminar: ${t.message}"
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
                    // Si falla el registro, el repositorio ya lo guardó localmente (sincronizado = false)
                    // Así que refrescamos y cerramos el diálogo con éxito
                    cargarVacunas(vacuna.animal_id)
                    onSuccess()
                }
        }
    }
}