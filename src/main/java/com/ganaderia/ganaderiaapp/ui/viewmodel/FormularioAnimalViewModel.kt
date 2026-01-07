package com.ganaderia.ganaderiaapp.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganaderia.ganaderiaapp.data.model.*
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FormularioAnimalViewModel(
    private val repository: GanadoRepository,
    private val context: Context
) : ViewModel() {

    // Ahora manejamos listas de Strings (Identificaciones)
    private val _hembras = MutableStateFlow<List<String>>(emptyList())
    val hembras = _hembras.asStateFlow()

    private val _machos = MutableStateFlow<List<String>>(emptyList())
    val machos = _machos.asStateFlow()

    private val _animalActual = MutableStateFlow<Animal?>(null)
    val animalActual = _animalActual.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _operacionExitosa = MutableSharedFlow<Boolean>()
    val operacionExitosa = _operacionExitosa.asSharedFlow()

    init {
        cargarOpcionesPadres()
    }

    fun cargarOpcionesPadres() {
        viewModelScope.launch {
            repository.getAnimalesSinFiltros().onSuccess { lista ->
                _hembras.value = lista
                    .filter { it.sexo.equals("Hembra", ignoreCase = true) }
                    .map { it.identificacion } // Solo la identificación

                _machos.value = lista
                    .filter { it.sexo.equals("Macho", ignoreCase = true) }
                    .map { it.identificacion }
            }
        }
    }

    fun cargarAnimal(localId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAnimalByLocalId(localId)
                .onSuccess { animal ->
                    _animalActual.value = animal
                    _error.value = null
                }
                .onFailure { e ->
                    _error.value = "Error al cargar animal: ${e.message}"
                }
            _isLoading.value = false
        }
    }

    fun guardarAnimal(animal: AnimalRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val actual = _animalActual.value
            val resultado = if (actual == null) {
                repository.registrarAnimal(animal, context)
            } else {
                repository.actualizarAnimal(actual.localId, animal, context)
            }

            resultado
                .onSuccess {
                    _isLoading.value = false
                    _operacionExitosa.emit(true)
                }
                .onFailure { e ->
                    _isLoading.value = false
                    _error.value = e.message ?: "Error al guardar"
                }
        }
    }
}