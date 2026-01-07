package com.ganaderia.ganaderiaapp.data.model

import com.google.gson.annotations.SerializedName

data class KPIs(
    @SerializedName("total_animales")
    val total_animales: Int,

    @SerializedName("peso_promedio")
    val peso_promedio: String?,

    @SerializedName("total_hembras")
    val total_hembras: String?,

    @SerializedName("total_machos")
    val total_machos: String?,

    @SerializedName("en_treatment")
    val en_tratamiento: String?
)