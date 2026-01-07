package com.ganaderia.ganaderiaapp.data.model

import com.ganaderia.ganaderiaapp.data.local.entities.AnimalEntity
import com.ganaderia.ganaderiaapp.data.local.entities.KPIsEntity
import com.ganaderia.ganaderiaapp.data.local.entities.VacunaEntity

// 1. Animal -> Entity
fun Animal.toEntity(sincronizado: Boolean, localId: Int = 0): AnimalEntity {
    return AnimalEntity(
        localId = localId,
        id = this.id,
        identificacion = this.identificacion,
        raza = this.raza,
        sexo = this.sexo,
        categoria = this.categoria,
        fecha_nacimiento = this.fecha_nacimiento,
        fecha_ingreso = this.fecha_ingreso,
        peso_actual = this.peso_actual.toString(), // Convertir a String si la Entity lo requiere
        estado_salud = this.estado_salud,
        notas = this.notas,
        sincronizado = sincronizado,
        edad_meses = this.edad_meses ?: 0,
        activo = this.activo
        // Elimina madre_identificacion/padre_identificacion si no existen en AnimalEntity
    )
}

// 2. Entity -> Model
fun AnimalEntity.toModel(): Animal {
    return Animal(
        localId = this.localId,
        id = this.id ?: 0,
        identificacion = this.identificacion,
        raza = this.raza,
        sexo = this.sexo,
        categoria = this.categoria,
        fecha_nacimiento = this.fecha_nacimiento,
        fecha_ingreso = this.fecha_ingreso,
        peso_actual = this.peso_actual?.toDoubleOrNull() ?: 0.0, // Conversión segura
        estado_salud = this.estado_salud,
        notas = this.notas,
        edad_meses = this.edad_meses,
        activo = this.activo,
        sincronizado = this.sincronizado
        // Elimina los campos 'madre_raza', 'padre_raza', etc., si Animal no los tiene
    )
}

// 3. Entity -> Request (Para API)
fun AnimalEntity.toRequest(): AnimalRequest {
    return AnimalRequest(
        identificacion = this.identificacion,
        raza = this.raza,
        sexo = this.sexo,
        categoria = this.categoria,
        fecha_nacimiento = this.fecha_nacimiento,
        fecha_ingreso = this.fecha_ingreso,
        peso_actual = this.peso_actual?.toDoubleOrNull(), // String -> Double
        estado_salud = this.estado_salud,
        madre_id = null,
        padre_id = null,
        notas = this.notas
    )
}

// 4. Request -> Entity
fun AnimalRequest.toEntity(sincronizado: Boolean = false, localId: Int = 0): AnimalEntity {
    return AnimalEntity(
        localId = localId,
        id = null,
        identificacion = this.identificacion,
        raza = this.raza,
        sexo = this.sexo,
        categoria = this.categoria,
        fecha_nacimiento = this.fecha_nacimiento,
        fecha_ingreso = this.fecha_ingreso ?: "",
        peso_actual = this.peso_actual?.toString() ?: "0.0", // Double -> String
        estado_salud = this.estado_salud,
        notas = this.notas,
        sincronizado = sincronizado,
        edad_meses = 0,
        activo = 1
    )
}

// 5. VacunaRequest -> Entity
fun VacunaRequest.toEntity(sincronizado: Boolean = false): VacunaEntity {
    return VacunaEntity(
        id = 0,
        animal_id = this.animal_id,
        nombre_vacuna = this.nombre_vacuna,
        fecha_aplicacion = this.fecha_aplicacion,
        dosis = this.dosis,
        sincronizado = sincronizado,
        lote = this.lote ?: "",
        veterinario = this.veterinario ?: "",
        proxima_dosis = this.proxima_dosis ?: "",
        observaciones = this.observaciones ?: ""
    )
}

// 6. KPIs
fun KPIsEntity.toDomain(): KPIs {
    return KPIs(
        total_animales = this.total_animales,
        peso_promedio = this.peso_promedio,
        total_hembras = this.total_hembras,
        total_machos = this.total_machos,
        en_tratamiento = this.en_tratamiento
    )
}

fun KPIs.toEntity(): KPIsEntity {
    return KPIsEntity(
        id = 1,
        total_animales = this.total_animales,
        peso_promedio = this.peso_promedio ?: "0",
        total_hembras = this.total_hembras ?: "0",
        total_machos = this.total_machos ?: "0",
        en_tratamiento = this.en_tratamiento ?: "0"
    )
}