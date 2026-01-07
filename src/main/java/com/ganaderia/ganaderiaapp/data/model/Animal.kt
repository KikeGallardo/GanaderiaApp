package com.ganaderia.ganaderiaapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Animal(
    val localId: Int = 0,
    val id: Int = 0,
    val identificacion: String,
    val raza: String,
    val sexo: String,
    val categoria: String,
    val fecha_nacimiento: String,
    val fecha_ingreso: String,
    val peso_actual: String?,
    val estado_salud: String,
    val notas: String?,
    val madre_identificacion: String?,
    val madre_raza: String?,
    val padre_identificacion: String?,
    val padre_raza: String?,
    val edad_meses: Int,
    val activo: Int = 1,
    val sincronizado: Boolean = true,
    val madre_id: Int?,
    val padre_id: Int?
) : Parcelable

data class AnimalRequest(
    val identificacion: String,
    val raza: String,
    val sexo: String,
    val categoria: String,
    val fecha_nacimiento: String,
    val fecha_ingreso: String,
    val peso_actual: Double?,
    val estado_salud: String,
    // CAMBIADO A STRING PARA GUARDAR NOMBRES DIRECTAMENTE
    val madre_identificacion: String?,
    val padre_identificacion: String?,
    val notas: String?
)
@Parcelize
data class AnimalSimple(
    val id: Int,
    val identificacion: String,
    val raza: String
) : Parcelable
