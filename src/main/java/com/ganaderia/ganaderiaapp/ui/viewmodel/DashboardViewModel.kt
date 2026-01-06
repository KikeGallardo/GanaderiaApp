package com.ganaderia.ganaderiaapp.ui.viewmodel

// ============================================
// Archivo: ui/viewmodel/DashboardViewModel.kt
// ============================================

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganaderia.ganaderiaapp.data.model.KPIs
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val repository = GanadoRepository()

    private val _kpis = MutableStateFlow<KPIs?>(null)
    val kpis: StateFlow<KPIs?> = _kpis

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        cargarKPIs()
    }

    fun cargarKPIs() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Agregamos un pequeño delay de seguridad (opcional, solo para debug)
            // delay(500)

            repository.getKPIs()
                .onSuccess { datosRecibidos ->
                    println("DEBUG: Datos recibidos con éxito: $datosRecibidos")
                    _kpis.value = datosRecibidos
                    _isLoading.value = false // Solo quitamos carga si hubo éxito
                }
                .onFailure { exception ->
                    println("DEBUG: Error en repositorio: ${exception.message}")
                    _error.value = exception.message ?: "Error desconocido"
                    _isLoading.value = false // O si hubo error
                }
        }
    }
}