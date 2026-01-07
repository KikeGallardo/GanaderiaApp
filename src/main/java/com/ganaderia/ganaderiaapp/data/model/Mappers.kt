package com.ganaderia.ganaderiaapp.data.model

import com.ganaderia.ganaderiaapp.data.local.entities.AnimalEntity
import com.ganaderia.ganaderiaapp.data.local.entities.KPIsEntity
import com.ganaderia.ganaderiaapp.data.local.entities.VacunaEntity
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UTILIDADES DE FECHA
 */
fun formatearFechaAmigable(fechaIso: String?): String {
    if (fechaIso.isNullOrBlank()) return "Sin registrar"
    return try {
        val fechaLimpia = fechaIso.take(10)
        val localDate = LocalDate.parse(fechaLimpia)
        val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale("es", "ES"))
        localDate.format(formatter)
    } catch (e: Exception) {
        fechaIso
    }
}

/**
 * MAPPERS PARA ANIMALES
 */

// 1. Convertir de Modelo (UI) a Entidad (Base de Datos)
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
        peso_actual = this.peso_actual?.toDoubleOrNull(), // String? -> Double?
        estado_salud = this.estado_salud,
        notas = this.notas,
        edad_meses = this.edad_meses,
<<<<<<< HEAD
        madre_id = this.madre_id,
        padre_id = this.padre_id,
        madre_identificacion = this.madre_identificacion,
        padre_identificacion = this.padre_identificacion,
=======
        madre_id = null, // Ajustar según lógica de negocio si es necesario
        padre_id = null,
>>>>>>> parent of 4eebf21 (Final con detalles)
        sincronizado = sincronizado,
        activo = 1
    )
}

// 2. Convertir de Entidad (Base de Datos) a Modelo (UI)
fun AnimalEntity.toModel(): Animal {
    // Cálculo dinámico de edad para evitar el bug de "0 meses"
    val edadCalculada = try {
        val fechaNac = LocalDate.parse(this.fecha_nacimiento.take(10))
        val periodo = Period.between(fechaNac, LocalDate.now())
        (periodo.years * 12) + periodo.months
    } catch (e: Exception) {
        0
    }

    return Animal(
        localId = this.localId,
        id = this.id ?: 0,
        identificacion = this.identificacion,
        raza = this.raza,
        sexo = this.sexo,
        categoria = this.categoria,
        fecha_nacimiento = this.fecha_nacimiento,
        fecha_ingreso = this.fecha_ingreso,
        peso_actual = this.peso_actual?.toString(), // Double? -> String?
        estado_salud = this.estado_salud,
        notas = this.notas,
<<<<<<< HEAD
        madre_identificacion = this.madre_identificacion,
        padre_identificacion = this.padre_identificacion,
=======
        madre_identificacion = null, // Estos campos no existen en AnimalEntity actualmente
>>>>>>> parent of 4eebf21 (Final con detalles)
        madre_raza = null,
        padre_raza = null,
        edad_meses = edadCalculada, // Se usa el valor calculado
        activo = this.activo,
        sincronizado = this.sincronizado,
        madre_id = this.madre_id,
        padre_id = this.padre_id
    )
}

// 3. Convertir de Entidad (Base de Datos) a Request (API)
fun AnimalEntity.toRequest(): AnimalRequest {
    return AnimalRequest(
        identificacion = this.identificacion,
        raza = this.raza,
        sexo = this.sexo,
        categoria = this.categoria,
        fecha_nacimiento = this.fecha_nacimiento,
        fecha_ingreso = this.fecha_ingreso,
        peso_actual = this.peso_actual,
        estado_salud = this.estado_salud,
<<<<<<< HEAD
        madre_identificacion = this.madre_identificacion,
        padre_identificacion = this.padre_identificacion,
        notas = this.notas ?: ""
=======
        madre_id = this.madre_id,
        padre_id = this.padre_id,
        notas = this.notas ?: "" // Enviar string vacío en lugar de null si da problemas
>>>>>>> parent of 4eebf21 (Final con detalles)
    )
}

// 4. Convertir de Request (API) a Entidad (Base de Datos)
fun AnimalRequest.toEntity(sincronizado: Boolean, localId: Int = 0): AnimalEntity {
    return AnimalEntity(
        localId = localId,
        id = null,
        identificacion = this.identificacion,
        raza = this.raza,
        sexo = this.sexo,
        categoria = this.categoria,
        fecha_nacimiento = this.fecha_nacimiento,
        fecha_ingreso = this.fecha_ingreso,
        peso_actual = this.peso_actual, // Ya es Double? en el Request
        estado_salud = this.estado_salud,
        notas = this.notas,
<<<<<<< HEAD
        madre_identificacion = this.madre_identificacion,
        padre_identificacion = this.padre_identificacion,
        sincronizado = sincronizado,
=======
        sincronizado = sincronizado,
        edad_meses = 0,
        madre_id = this.madre_id,
        padre_id = this.padre_id,
>>>>>>> parent of 4eebf21 (Final con detalles)
        activo = 1
    )
}

/**
 * MAPPERS PARA VACUNAS
 */
fun VacunaRequest.toEntity(sincronizado: Boolean = false): VacunaEntity {
    return VacunaEntity(
        localId = 0, // Generado automáticamente por Room
        id = null,   // Se llenará cuando la API responda
        animal_id = this.animal_id,
        nombre_vacuna = this.nombre_vacuna,
        fecha_aplicacion = this.fecha_aplicacion,
        dosis = this.dosis,
        lote = this.lote,
        veterinario = this.veterinario,
        proxima_dosis = this.proxima_dosis,
        observaciones = this.observaciones,
        sincronizado = sincronizado
    )
}

/**
 * MAPPERS PARA KPIs
 */
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
        id = 1, // ID único para mantener siempre una sola fila de caché
        total_animales = this.total_animales,
        peso_promedio = this.peso_promedio ?: "0",
        total_hembras = this.total_hembras ?: "0",
        total_machos = this.total_machos ?: "0",
        en_tratamiento = this.en_tratamiento ?: "0"
    )
}