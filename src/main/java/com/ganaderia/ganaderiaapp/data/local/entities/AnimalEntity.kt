package com.ganaderia.ganaderiaapp.data.local.entities

import android.R
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animales")
data class AnimalEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val id: Int?,
    val identificacion: String,
    val raza: String,
    val sexo: String,
    val categoria: String,
    val fecha_nacimiento: String,
    val fecha_ingreso: String,
    val peso_actual: Double?,
    val estado_salud: String,
    val notas: String?,
    val sincronizado: Boolean = false,  // 🔧 CAMBIADO: Boolean -> Int (0 = false, 1 = true)
    val edad_meses: Int = 0,
    val madre_id: Int? = null,
    val padre_id: Int? = null,
    val activo: Int = 1
)

