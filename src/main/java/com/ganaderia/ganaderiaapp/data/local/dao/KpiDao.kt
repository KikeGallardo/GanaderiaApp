package com.ganaderia.ganaderiaapp.data.local.dao

import androidx.room.*
import com.ganaderia.ganaderiaapp.data.local.entities.KPIsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KpiDao {

    // Recupera los KPIs locales para mostrar aunque no haya internet
    @Query("SELECT * FROM dashboard_kpis WHERE id = 1")
    fun getKPIs(): Flow<KPIsEntity?>

    // Guarda los KPIs que vienen del servidor
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKPIs(kpis: KPIsEntity)

    // Opcional: Para limpiar los datos al cerrar sesión
    @Query("DELETE FROM dashboard_kpis")
    suspend fun clearKPIs()

    // Insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(kpis: KPIsEntity)

    companion object
}