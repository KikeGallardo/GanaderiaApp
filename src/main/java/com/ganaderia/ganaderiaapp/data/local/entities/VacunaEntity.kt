package com.ganaderia.ganaderiaapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vacunas")
data class VacunaEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val id: Int? = null, // ID del servidor
    val animal_id: Int,
    val nombre_vacuna: String,
    val fecha_aplicacion: String,
    val dosis: String?,
    val lote: String?,
    val veterinario: String?,
    val proxima_dosis: String?,
    val observaciones: String?,
    val sincronizado: Boolean = true
)