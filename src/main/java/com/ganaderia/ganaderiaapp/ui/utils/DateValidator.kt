package com.ganaderia.ganaderiaapp.ui.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Utilidad para validación y formateo de fechas
 * Ubicación: src/main/java/com/ganaderia/ganaderiaapp/ui/utils/DateValidator.kt
 */
object DateValidator {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Valida que una fecha tenga formato YYYY-MM-DD y sea válida
     */
    fun validarFormato(fecha: String): Boolean {
        if (!fecha.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            return false
        }

        return try {
            LocalDate.parse(fecha, formatter)
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }

    /**
     * Valida que la fecha de nacimiento no sea futura
     */
    fun validarFechaNacimiento(fecha: String): Pair<Boolean, String?> {
        if (!validarFormato(fecha)) {
            return false to "Formato inválido. Use AAAA-MM-DD"
        }

        val fechaNac = LocalDate.parse(fecha, formatter)
        val hoy = LocalDate.now()

        return when {
            fechaNac.isAfter(hoy) -> false to "La fecha no puede ser futura"
            fechaNac.isBefore(hoy.minusYears(30)) -> false to "Fecha muy antigua (>30 años)"
            else -> true to null
        }
    }

    /**
     * Valida que fecha_ingreso >= fecha_nacimiento
     */
    fun validarFechaIngreso(
        fechaNacimiento: String,
        fechaIngreso: String
    ): Pair<Boolean, String?> {
        if (!validarFormato(fechaNacimiento) || !validarFormato(fechaIngreso)) {
            return false to "Formato inválido en alguna fecha"
        }

        val fechaNac = LocalDate.parse(fechaNacimiento, formatter)
        val fechaIng = LocalDate.parse(fechaIngreso, formatter)

        return when {
            fechaIng.isBefore(fechaNac) ->
                false to "Fecha de ingreso no puede ser antes del nacimiento"
            fechaIng.isAfter(LocalDate.now()) ->
                false to "Fecha de ingreso no puede ser futura"
            else -> true to null
        }
    }

    /**
     * Formatea fecha para mostrar en UI (DD/MM/YYYY)
     */
    fun formatearParaUI(fecha: String): String {
        return try {
            val date = LocalDate.parse(fecha, formatter)
            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } catch (e: Exception) {
            fecha
        }
    }

    /**
     * Calcula edad en meses desde fecha de nacimiento
     */
    fun calcularEdadMeses(fechaNacimiento: String): Int? {
        return try {
            val fechaNac = LocalDate.parse(fechaNacimiento, formatter)
            val hoy = LocalDate.now()
            val años = hoy.year - fechaNac.year
            val meses = hoy.monthValue - fechaNac.monthValue
            años * 12 + meses
        } catch (e: Exception) {
            null
        }
    }
}

/*
==============================================
EJEMPLO DE USO EN FormularioAnimalScreen.kt:
==============================================

// 1. Agregar import al inicio del archivo:
import com.ganaderia.ganaderiaapp.ui.utils.DateValidator

// 2. En el botón de guardar, ANTES de crear AnimalRequest:

if (identificacion.isEmpty() || fechaNacimiento.isEmpty() || fechaIngreso.isEmpty()) {
    mostrarErrores = true
    return@Button
}

// Validar fecha de nacimiento
val (fechaNacValida, errorFechaNac) = DateValidator.validarFechaNacimiento(fechaNacimiento)
if (!fechaNacValida) {
    mostrarErrores = true
    // Mostrar error específico al usuario
    return@Button
}

// Validar fecha de ingreso
val (fechaIngValida, errorFechaIng) = DateValidator.validarFechaIngreso(fechaNacimiento, fechaIngreso)
if (!fechaIngValida) {
    mostrarErrores = true
    // Mostrar error específico al usuario
    return@Button
}

// Continuar con la creación del AnimalRequest...

==============================================
EJEMPLO DE USO EN DetalleAnimalScreen.kt:
==============================================

// Mostrar fechas formateadas en la UI:
InfoRow(
    Icons.Default.DateRange,
    "Nacimiento",
    DateValidator.formatearParaUI(animal.fecha_nacimiento)
)

// Calcular y mostrar edad:
val edadMeses = DateValidator.calcularEdadMeses(animal.fecha_nacimiento)
if (edadMeses != null) {
    InfoRow(
        Icons.Default.CalendarToday,
        "Edad",
        "$edadMeses meses (${edadMeses / 12} años)"
    )
}
*/