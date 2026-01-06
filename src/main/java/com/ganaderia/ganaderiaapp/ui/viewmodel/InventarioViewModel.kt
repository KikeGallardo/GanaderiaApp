// ============================================
// Archivo: ui/viewmodel/InventarioViewModel.kt
// ============================================
package com.ganaderia.ganaderiaapp.ui.viewmodel
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganaderia.ganaderiaapp.data.model.Animal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InventarioViewModel : ViewModel() {
    private val repository = GanadoRepository()

    private val _animales = MutableStateFlow<List<Animal>>(emptyList())
    val animales: StateFlow<List<Animal>> = _animales

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda

    private val _razaSeleccionada = MutableStateFlow<String?>(null)
    private val _sexoSeleccionado = MutableStateFlow<String?>(null)
    private val _categoriaSeleccionada = MutableStateFlow<String?>(null)

    init {
        cargarAnimales()
    }

    fun cargarAnimales() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getAnimales(
                busqueda = _busqueda.value.ifEmpty { null },
                raza = _razaSeleccionada.value,
                sexo = _sexoSeleccionado.value,
                categoria = _categoriaSeleccionada.value
            )
                .onSuccess { _animales.value = it }
                .onFailure { _error.value = it.message ?: "Error al cargar animales" }

            _isLoading.value = false
        }
    }

    fun buscar(texto: String) {
        _busqueda.value = texto
        cargarAnimales()
    }

    fun filtrarPorRaza(raza: String?) {
        _razaSeleccionada.value = raza
        cargarAnimales()
    }

    fun filtrarPorSexo(sexo: String?) {
        _sexoSeleccionado.value = sexo
        cargarAnimales()
    }

    fun filtrarPorCategoria(categoria: String?) {
        _categoriaSeleccionada.value = categoria
        cargarAnimales()
    }

    fun eliminarAnimal(id: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.eliminarAnimal(id)
                .onSuccess {
                    cargarAnimales()
                    onSuccess()
                }
                .onFailure { _error.value = it.message }
        }
    }
}