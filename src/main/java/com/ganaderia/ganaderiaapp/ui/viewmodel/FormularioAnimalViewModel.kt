// ============================================
// Archivo: ui/viewmodel/FormularioAnimalViewModel.kt
// ============================================
package com.ganaderia.ganaderiaapp.ui.viewmodel
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganaderia.ganaderiaapp.data.model.AnimalRequest
import com.ganaderia.ganaderiaapp.data.model.AnimalSimple
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FormularioAnimalViewModel : ViewModel() {
    private val repository = GanadoRepository()

    private val _hembras = MutableStateFlow<List<AnimalSimple>>(emptyList())
    val hembras: StateFlow<List<AnimalSimple>> = _hembras

    private val _machos = MutableStateFlow<List<AnimalSimple>>(emptyList())
    val machos: StateFlow<List<AnimalSimple>> = _machos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        cargarPadres()
    }

    private fun cargarPadres() {
        viewModelScope.launch {
            _isLoading.value = true

            repository.getHembras()
                .onSuccess { _hembras.value = it }

            repository.getMachos()
                .onSuccess { _machos.value = it }

            _isLoading.value = false
        }
    }

    fun crearAnimal(animal: AnimalRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.crearAnimal(animal)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { _error.value = it.message }

            _isLoading.value = false
        }
    }

    fun actualizarAnimal(id: Int, animal: AnimalRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.actualizarAnimal(id, animal)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { _error.value = it.message }

            _isLoading.value = false
        }
    }
}