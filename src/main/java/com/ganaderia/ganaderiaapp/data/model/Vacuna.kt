package com.ganaderia.ganaderiaapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Vacuna(
    val id: Int = 0,
    val animal_id: Int,
    val nombre_vacuna: String,
    val fecha_aplicacion: String,
    val dosis: String?,
    val lote: String?,
    val veterinario: String?,
    val proxima_dosis: String?,
    val observaciones: String?
) : Parcelable

data class VacunaRequest(
    val animal_id: Int,
    val nombre_vacuna: String,
    val fecha_aplicacion: String,
    val dosis: String?,
    val lote: String?,
    val veterinario: String?,
    val proxima_dosis: String?,
    val observaciones: String?
)