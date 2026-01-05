package com.ganaderia.inventarioapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName

@Entity(tableName = "animales")
@TypeConverters(VacunaConverter::class) // Necesario para guardar la lista de vacunas
data class Animal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Cambiado a 0 para que Room genere el ID automáticamente
    val identificacion: String,
    val raza: String,
    val sexo: String,
    val categoria: String,
    val fechaNacimiento: String,
    val madreId: String,
    val padreId: String,
    val potreroUbicacion: String,
    val fechaIngreso: String,
    val precioCompra: String,
    val notasObservaciones: String,
    val vacunas: MutableList<Vacuna> = mutableListOf(),
    val eliminado: Boolean = false,
    val sincronizado: Boolean = false // Añadido para el sistema de sincronización
)
{ // Dentro de la clase Animal, antes del companion object
    fun toStringData(): String {
        return "$id,$identificacion,$raza,$sexo,$categoria,$fechaNacimiento,$madreId,$padreId,$potreroUbicacion,$fechaIngreso,$precioCompra,$notasObservaciones,$eliminado,$sincronizado"
    }
    companion object {
        val RAZAS = listOf(
            "Brahman", "Angus", "Hereford", "Charolais", "Simmental",
            "Holstein", "Jersey", "Nelore", "Gyr", "Senepol",
            "Brangus", "Santa Gertrudis", "Criollo", "Mestizo", "Otra"
        )

        val SEXOS = listOf("Macho", "Hembra")

        val CATEGORIAS = listOf(
            "Ternero/a", "Novillo/a", "Torete", "Vaquilla",
            "Toro", "Vaca", "Buey"
        )
        fun fromStringData(data: String): Animal? {
            val parts = data.split(",")
            return try {
                if (parts.size < 15) return null
                Animal(
                    id = parts[0].toLong(),
                    identificacion = parts[1],
                    raza = parts[2],
                    sexo = parts[3],
                    categoria = parts[4],
                    fechaNacimiento = parts[5],
                    madreId = parts[6],
                    padreId = parts[7],
                    potreroUbicacion = parts[8],
                    fechaIngreso = parts[9],
                    precioCompra = parts[10],
                    notasObservaciones = parts[11],
                    eliminado = parts[12].toBoolean(),
                    sincronizado = parts[13].toBoolean()
                    // Nota: Las vacunas requieren lógica adicional si se guardan en String
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class Vacuna(
    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("fecha")
    val fecha: String,

    @SerializedName("proximaDosis")
    val proximaDosis: String
) {
    companion object {
        val TIPOS_VACUNAS = listOf(
            "Aftosa",
            "Brucelosis",
            "Rabia",
            "Carbunco",
            "Clostridiosis",
            "IBR",
            "DVB",
            "Leptospirosis",
            "Paratuberculosis",
            "Respiratoria",
            "Reproductiva"
        )
    }
}