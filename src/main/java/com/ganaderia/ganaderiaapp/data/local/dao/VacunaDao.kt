package com.ganaderia.ganaderiaapp.data.local.dao

import androidx.room.*
import com.ganaderia.ganaderiaapp.data.local.entities.VacunaEntity

@Dao
interface VacunaDao {
    @Query("SELECT * FROM vacunas WHERE animal_id = :animalId")
    suspend fun getVacunasByAnimal(animalId: Int): List<VacunaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVacuna(vacuna: VacunaEntity)

    @Query("SELECT * FROM vacunas WHERE sincronizado = 0")
    suspend fun getVacunasPendientes(): List<VacunaEntity>
}