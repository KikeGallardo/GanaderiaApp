package com.ganaderia.ganaderiaapp.ui.viewmodel

import android.util.Log
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

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        cargarKPIs()
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

    fun forzarSincronizacion() {
        viewModelScope.launch {
            _isSyncing.value = true
            _error.value = null

            Log.d("DashboardViewModel", "Iniciando sincronización forzada")

            try {
                repository.forceSync()
                repository.sincronizarKPIs()

                // Recargar KPIs después de sincronizar
                repository.getKPIsLocales().collect { kpis ->
                    if (kpis != null) {
                        _kpis.value = kpis
                        Log.d("DashboardViewModel", "KPIs actualizados después de sincronización")
                    }
                    _isSyncing.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
                _isSyncing.value = false
                Log.e("DashboardViewModel", "Error en sincronización forzada", e)
            }
        }
    }
}