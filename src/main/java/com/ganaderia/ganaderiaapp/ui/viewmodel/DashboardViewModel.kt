package com.ganaderia.ganaderiaapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganaderia.ganaderiaapp.data.model.KPIs
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: GanadoRepository) : ViewModel() {

    private val _kpis = MutableStateFlow<KPIs?>(null)
    val kpis: StateFlow<KPIs?> = _kpis

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Nuevo estado para el botón de sincronización forzada
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        cargarKPIs()
    }

    fun cargarKPIs() {
        viewModelScope.launch {
            // 1. Escuchar la DB local permanentemente
            repository.getKPIsLocales().collect { localKpis ->
                if (localKpis != null) {
                    _kpis.value = localKpis
                    _isLoading.value = false
                }
            }
        }

        viewModelScope.launch {
            try {
                if (kpis.value == null) _isLoading.value = true
                repository.sincronizarKPIs()
            } catch (e: Exception) {
                _error.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- NUEVA FUNCIÓN PARA EL BOTÓN ---
    fun forzarSincronizacion() {
        viewModelScope.launch {
            _isSyncing.value = true
            _error.value = null
            try {
                // Llamamos a la nueva función del repositorio que actualiza TODO
                repository.forceSync()
                // Opcional: recargar explícitamente después de la sincronización
                repository.sincronizarKPIs()
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
}