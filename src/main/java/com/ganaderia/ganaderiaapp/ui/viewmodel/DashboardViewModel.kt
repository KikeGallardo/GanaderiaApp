package com.ganaderia.ganaderiaapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganaderia.ganaderiaapp.data.model.KPIs
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: GanadoRepository) : ViewModel() {

    private val _kpis = MutableStateFlow<KPIs?>(null)
    val kpis: StateFlow<KPIs?> = _kpis

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var autoRefreshJob: Job? = null

    init {
        cargarKPIs()
    }

    // 🔧 NUEVO: Auto-refresh cada 30 segundos
    /*
    private fun iniciarAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            delay(5000) // Esperar 5s después de carga inicial

            while (isActive) {
                try {
                    Log.d("DashboardViewModel", "🔄 Auto-refresh ejecutándose...")
                    sincronizarEnSegundoPlano()
                    delay(30000) // 30 segundos
                } catch (e: Exception) {
                    Log.e("DashboardViewModel", "Error en auto-refresh", e)
                    delay(60000) // Si hay error, esperar 1 minuto
                }
            }
        }
    }*/

private suspend fun sincronizarEnSegundoPlano() {
    try {
        repository.forceSync()
        repository.getKPIsLocales().collect { localKpis ->
            if (localKpis != null) {
                _kpis.value = localKpis
            }
        }
    } catch (e: Exception) {
        Log.e("DashboardViewModel", "Error en sincronización de fondo", e)
    }
}

fun cargarKPIs() {
    viewModelScope.launch {
        _isLoading.value = true
        _error.value = null

        Log.d("DashboardViewModel", "Cargando KPIs")

        try {
            repository.getKPIs().collect { kpis ->
                _kpis.value = kpis
                _isLoading.value = false
                Log.d("DashboardViewModel", "KPIs cargados: ${kpis.total_animales} animales")
            }
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Error cargando KPIs", e)
            repository.getKPIsLocales().collect { localKpis ->
                if (localKpis != null) {
                    _kpis.value = localKpis
                    _error.value = "Mostrando datos guardados (sin conexión)"
                    Log.d("DashboardViewModel", "Usando KPIs locales")
                } else {
                    _error.value = "No hay conexión y no hay datos guardados"
                }
                _isLoading.value = false
            }
        }
    }
}

// Mantener función manual por si el usuario quiere forzar
fun forzarSincronizacion() {
    viewModelScope.launch {
        _isSyncing.value = true
        _error.value = null

        Log.d("DashboardViewModel", "Sincronización forzada manual")

        try {
            repository.forceSync()
            repository.sincronizarKPIs()

            repository.getKPIsLocales().collect { kpis ->
                if (kpis != null) {
                    _kpis.value = kpis
                    Log.d("DashboardViewModel", "KPIs actualizados")
                }
                _isSyncing.value = false
            }
        } catch (e: Exception) {
            _error.value = "Error de conexión: ${e.message}"
            _isSyncing.value = false
            Log.e("DashboardViewModel", "Error en sincronización", e)
        }
    }
}

override fun onCleared() {
    super.onCleared()
    autoRefreshJob?.cancel()
}
}
