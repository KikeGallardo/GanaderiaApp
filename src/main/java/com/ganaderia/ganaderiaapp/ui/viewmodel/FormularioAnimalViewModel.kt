package com.ganaderia.ganaderiaapp.ui.viewmodel

import android.content.Context
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

    private val _hembras = MutableStateFlow<List<AnimalSimple>>(emptyList())
    val hembras = _hembras.asStateFlow()

    private val _machos = MutableStateFlow<List<AnimalSimple>>(emptyList())
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
            repository.getAnimalesSinFiltros()
                .onSuccess { lista ->
                    _hembras.value = lista
                        .filter { it.sexo.equals("Hembra", ignoreCase = true) }
                        .map { AnimalSimple(it.localId, it.identificacion, it.raza) }

                    _machos.value = lista
                        .filter { it.sexo.equals("Macho", ignoreCase = true) }
                        .map { AnimalSimple(it.localId, it.identificacion, it.raza) }
                }
                .onFailure {
                    // Modo offline
                }
        }
    }

    fun cargarAnimal(localId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAnimalByLocalId(localId)
                .onSuccess {
                    _animalActual.value = it
                    _error.value = null
                }
                .onFailure {
                    _error.value = "Error al cargar animal: ${it.message}"
                }
            _isLoading.value = false
        }
    }

    fun guardarAnimal(animal: AnimalRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val resultado = if (_animalActual.value == null) {
                // CORRECCIÓN: Pasar contexto para sincronización automática
                repository.registrarAnimal(animal, context)
            } else {
                // CORRECCIÓN: Pasar contexto para sincronización automática
                repository.actualizarAnimalByLocalId(_animalActual.value!!.localId, animal, context)
            }

            resultado
                .onSuccess {
                    _isLoading.value = false
                    _operacionExitosa.emit(true)
                }
                .onFailure {
                    _isLoading.value = false
                    _operacionExitosa.emit(true)
                }
        }
    }

    fun limpiarError() {
        _error.value = null
    }
}