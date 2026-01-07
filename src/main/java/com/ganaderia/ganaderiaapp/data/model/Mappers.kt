package com.ganaderia.ganaderiaapp.data.model

import com.ganaderia.ganaderiaapp.data.local.entities.AnimalEntity
import com.ganaderia.ganaderiaapp.data.local.entities.KPIsEntity
import com.ganaderia.ganaderiaapp.data.local.entities.VacunaEntity

// Conversión de Animal (API) a AnimalEntity (BD Local)
fun Animal.toEntity(sincronizado: Boolean, localId: Int = 0): AnimalEntity {
    return AnimalEntity(
        localId = localId, // <--- Ahora aceptamos el ID de Room
        id = this.id,      // El ID del servidor
        identificacion = this.identificacion,
        raza = this.raza,
        sexo = this.sexo,
        categoria = this.categoria,
        fecha_nacimiento = this.fecha_nacimiento,
        fecha_ingreso = this.fecha_ingreso,
        peso_actual = this.peso_actual,
        estado_salud = this.estado_salud,
        notas = this.notas,
        sincronizado = sincronizado,
        edad_meses = this.edad_meses ?: 0,
        madre_identificacion = this.madre_identificacion,
        padre_identificacion = this.padre_identificacion,
        activo = this.activo
    )
}

// Conversión de AnimalEntity (BD Local) a Animal (App/UI)
// 1. Corregir el paso de Entity a Model (UI)
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
        peso_actual = this.peso_actual,
        estado_salud = this.estado_salud,
        notas = this.notas,
        edad_meses = this.edad_meses,
        madre_identificacion = this.madre_identificacion,
        madre_raza = this.madre_raza,
        padre_identificacion = this.padre_identificacion,
        padre_raza = this.padre_raza,
        activo = this.activo,
        sincronizado = this.sincronizado // <--- CAMBIO: Usar el valor real de la BD
    )
}

fun AnimalEntity.toRequest(): AnimalRequest {
    return AnimalRequest(
        identificacion = this.identificacion,
        raza = this.raza,
        sexo = this.sexo,
        categoria = this.categoria,
        fecha_nacimiento = this.fecha_nacimiento,
        fecha_ingreso = this.fecha_ingreso,
        peso_actual = this.peso_actual?.toDoubleOrNull(), // Convertimos String a Double para la API
        estado_salud = this.estado_salud,
        madre_id = null, // Opcional, según tu lógica
        padre_id = null,
        notas = this.notas
    )
}

// ESTA ES LA FUNCIÓN QUE TE FALTABA PARA QUITAR EL ROJO EN EL REPOSITORIO
fun VacunaRequest.toEntity(sincronizado: Boolean = false): VacunaEntity {
    return VacunaEntity(
        id = 0, // Autoincrementable
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

// Conversión de AnimalRequest (Lo que envías) a AnimalEntity (BD Local) para modo Offline
// 2. Corregir AnimalRequest a Entity (Para edición o guardado local)
fun AnimalRequest.toEntity(sincronizado: Boolean = false, localId: Int = 0): AnimalEntity {
    return AnimalEntity(
        localId = localId, // <--- CAMBIO: Si pasamos el localId, Room actualizará en lugar de duplicar
        id = null,
        identificacion = this.identificacion,
        raza = this.raza,
        sexo = this.sexo,
        categoria = this.categoria,
        fecha_nacimiento = this.fecha_nacimiento,
        fecha_ingreso = this.fecha_ingreso ?: "",
        peso_actual = this.peso_actual?.toString() ?: "0",
        estado_salud = this.estado_salud,
        notas = this.notas,
        sincronizado = sincronizado,
        edad_meses = 0,
        madre_identificacion = null,
        madre_raza = null,
        padre_identificacion = null,
        padre_raza = null,
        activo = 1
    )
}


// Convierte de Base de Datos (Entity) a Objeto de Negocio (Domain/UI)
fun KPIsEntity.toDomain(): KPIs {
    return KPIs(
        total_animales = this.total_animales,
        peso_promedio = this.peso_promedio,
        total_hembras = this.total_hembras,
        total_machos = this.total_machos,
        en_tratamiento = this.en_tratamiento
    )
}

// Convierte de Servidor (KPIs) a Base de Datos (Entity) para guardar el caché
fun KPIs.toEntity(): KPIsEntity {
    return KPIsEntity(
        id = 1, // ID fijo para que siempre sea una sola fila
        total_animales = this.total_animales,
        peso_promedio = this.peso_promedio ?: "0",
        total_hembras = this.total_hembras ?: "0",
        total_machos = this.total_machos ?: "0",
        en_tratamiento = this.en_tratamiento ?: "0"
    )
}