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
    val peso_actual: Double?,  // CAMBIADO: de String a Double
    val estado_salud: String,
    val notas: String?,
<<<<<<< HEAD
    val sincronizado: Boolean = false,
    val edad_meses: Int = 0,
    val madre_id: Int? = null,
    val padre_id: Int? = null,
    // NUEVOS CAMPOS PARA NOMBRES
    val madre_identificacion: String?,
    val padre_identificacion: String?,
=======
    val sincronizado: Boolean = true,
    val edad_meses: Int = 0,
    val madre_id: Int? = null,  // CAMBIADO: de String a Int
    val padre_id: Int? = null,  // CAMBIADO: de String a Int
>>>>>>> parent of 4eebf21 (Final con detalles)
    val activo: Int = 1
)