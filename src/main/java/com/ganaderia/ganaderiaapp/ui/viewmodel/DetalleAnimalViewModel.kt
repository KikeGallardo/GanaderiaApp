package com.ganaderia.ganaderiaapp.ui.viewmodel

import android.util.Log
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

            Log.d("DetalleViewModel", "Cargando animal con localId: $localId")

            repository.getAnimalByLocalId(localId)
                .onSuccess { animal ->
                    _animal.value = animal
                    Log.d("DetalleViewModel", "Animal cargado: ${animal.identificacion}, sincronizado: ${animal.sincronizado}")

                    if (animal.id > 0) {
                        cargarVacunas(animal.id)
                    }

                    cargarCatalogo()
                }
                .onFailure { e ->
                    if (_animal.value == null) {
                        _error.value = "Error al cargar animal: ${e.message}"
                        Log.e("DetalleViewModel", "Error cargando animal", e)
                    }
                }

            _isLoading.value = false
        }
    }

    private fun cargarVacunas(animalId: Int) {
        viewModelScope.launch {
            Log.d("DetalleViewModel", "Cargando vacunas para animal ID: $animalId")
            repository.getVacunas(animalId)
                .onSuccess {
                    _vacunas.value = it
                    Log.d("DetalleViewModel", "Cargadas ${it.size} vacunas")
                }
                .onFailure {
                    Log.e("DetalleViewModel", "Error cargando vacunas", it)
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
                    Log.d("DetalleViewModel", "Nueva vacuna agregada al catálogo: $nombre")
                }
        }
    }

    fun eliminarAnimal(localId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true

            Log.d("DetalleViewModel", "Eliminando animal con localId: $localId")

            repository.eliminarAnimalByLocalId(localId)
                .onSuccess {
                    _isLoading.value = false
                    Log.d("DetalleViewModel", "Animal eliminado exitosamente")
                    onSuccess()
                }
                .onFailure {
                    _error.value = "Error al eliminar: ${it.message}"
                    _isLoading.value = false
                    Log.e("DetalleViewModel", "Error eliminando animal", it)
                }
        }
    }

    fun eliminarVacuna(vacunaId: Int, animalId: Int) {
        viewModelScope.launch {
            _isLoading.value = true

            Log.d("DetalleViewModel", "Eliminando vacuna ID: $vacunaId")

            repository.eliminarVacuna(vacunaId)
                .onSuccess {
                    cargarVacunas(animalId)
                    Log.d("DetalleViewModel", "Vacuna eliminada exitosamente")
                }
                .onFailure {
                    _error.value = "No se pudo eliminar: ${it.message}"
                    Log.e("DetalleViewModel", "Error eliminando vacuna", it)
                }

            _isLoading.value = false
        }
    }

    fun registrarVacuna(vacuna: VacunaRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            Log.d("DetalleViewModel", "Registrando vacuna: ${vacuna.nombre_vacuna}")

            repository.registrarVacuna(vacuna)
                .onSuccess {
                    cargarVacunas(vacuna.animal_id)
                    Log.d("DetalleViewModel", "Vacuna registrada exitosamente")
                    onSuccess()
                }
                .onFailure {
                    cargarVacunas(vacuna.animal_id)
                    Log.e("DetalleViewModel", "Error registrando vacuna", it)
                    onSuccess()
                }
        }
    }
}