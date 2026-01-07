package com.ganaderia.ganaderiaapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animales")
data class AnimalEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val id: Int? = null,
    val identificacion: String,
    val raza: String,
    val sexo: String,
    val categoria: String,
    val fecha_nacimiento: String,
    val fecha_ingreso: String,
    val peso_actual: String?,
    val estado_salud: String,
    val notas: String?,
    val sincronizado: Boolean = true,
    val edad_meses: Int = 0,
    val madre_identificacion: String? = null,
    val madre_raza: String? = null,
    val padre_identificacion: String? = null,
    val padre_raza: String? = null,
    val activo: Int = 1
)