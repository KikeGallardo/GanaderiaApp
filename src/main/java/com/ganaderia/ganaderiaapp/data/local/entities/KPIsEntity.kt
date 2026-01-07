package com.ganaderia.ganaderiaapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dashboard_kpis")
data class KPIsEntity(
    @PrimaryKey val id: Int = 1,
    val total_animales: Int,  // Cambia estos nombres si el error persiste
    val peso_promedio: String,
    val total_hembras: String,
    val total_machos: String,
    val en_tratamiento: String
)