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
        Log.d("FormularioViewModel", "ViewModel inicializado")
        cargarOpcionesPadres()
    }

    fun cargarOpcionesPadres() {
        viewModelScope.launch {
            Log.d("FormularioViewModel", "Cargando opciones de padres")
            repository.getAnimalesSinFiltros()
                .onSuccess { lista ->
                    _hembras.value = lista
                        .filter { it.sexo.equals("Hembra", ignoreCase = true) }
                        .map { AnimalSimple(it.localId, it.identificacion, it.raza) }

                    _machos.value = lista
                        .filter { it.sexo.equals("Macho", ignoreCase = true) }
                        .map { AnimalSimple(it.localId, it.identificacion, it.raza) }

                    Log.d("FormularioViewModel", "Cargadas ${_hembras.value.size} hembras y ${_machos.value.size} machos")
                }
                .onFailure { e ->
                    Log.e("FormularioViewModel", "Error cargando padres", e)
                }
        }
    }

    fun cargarAnimal(localId: Int) {
        viewModelScope.launch {
            Log.d("FormularioViewModel", "=== CARGANDO ANIMAL PARA EDITAR ===")
            Log.d("FormularioViewModel", "LocalId recibido: $localId")

            _isLoading.value = true
            repository.getAnimalByLocalId(localId)
                .onSuccess { animal ->
                    _animalActual.value = animal
                    _error.value = null
                    Log.d("FormularioViewModel", "Animal cargado exitosamente:")
                    Log.d("FormularioViewModel", "  - LocalId: ${animal.localId}")
                    Log.d("FormularioViewModel", "  - ServerId: ${animal.id}")
                    Log.d("FormularioViewModel", "  - Identificación: ${animal.identificacion}")
                    Log.d("FormularioViewModel", "  - Sincronizado: ${animal.sincronizado}")
                }
                .onFailure { e ->
                    _error.value = "Error al cargar animal: ${e.message}"
                    Log.e("FormularioViewModel", "Error cargando animal", e)
                }
            _isLoading.value = false
        }
    }

    fun guardarAnimal(animal: AnimalRequest) {
        viewModelScope.launch {
            Log.d("FormularioViewModel", "=== GUARDAR ANIMAL INICIADO ===")

            _isLoading.value = true
            _error.value = null

            val actual = _animalActual.value

            Log.d("FormularioViewModel", "Animal actual: ${if (actual == null) "null (CREAR NUEVO)" else "existe (EDITAR)"}")

            if (actual != null) {
                Log.d("FormularioViewModel", "Modo EDICIÓN:")
                Log.d("FormularioViewModel", "  - LocalId: ${actual.localId}")
                Log.d("FormularioViewModel", "  - ServerId: ${actual.id}")
                Log.d("FormularioViewModel", "  - Identificación: ${actual.identificacion}")
            }

            Log.d("FormularioViewModel", "Datos a guardar:")
            Log.d("FormularioViewModel", "  - Identificación: ${animal.identificacion}")
            Log.d("FormularioViewModel", "  - Raza: ${animal.raza}")
            Log.d("FormularioViewModel", "  - Peso: ${animal.peso_actual}")

            val resultado = if (actual == null) {
                Log.d("FormularioViewModel", "Ejecutando registrarAnimal()")
                repository.registrarAnimal(animal, context)
            } else {
                Log.d("FormularioViewModel", "Ejecutando actualizarAnimal() con localId=${actual.localId}")
                repository.actualizarAnimal(
                    actual.localId,
                    animal,
                    context
                )
            }

            resultado
                .onSuccess { animalGuardado ->
                    _isLoading.value = false
                    Log.d("FormularioViewModel", "✅ GUARDADO EXITOSO")
                    Log.d("FormularioViewModel", "Animal guardado - LocalId: ${animalGuardado.localId}, ID: ${animalGuardado.id}")
                    _operacionExitosa.emit(true)
                }
                .onFailure { e ->
                    _isLoading.value = false
                    _error.value = e.message ?: "Error al guardar el animal"
                    Log.e("FormularioViewModel", "❌ ERROR AL GUARDAR: ${e.message}", e)
                }

            Log.d("FormularioViewModel", "=== FIN GUARDAR ANIMAL ===")
        }
    }
}