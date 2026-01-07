package com.ganaderia.ganaderiaapp.data.local

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object ListadoAnimales : Screen("listado_animales")
    object DetalleAnimal : Screen("detalle_animal/{animalId}") {
        fun createRoute(animalId: Int) = "detalle_animal/$animalId"
    }
    object FormularioAnimal : Screen("formulario_animal?animalId={animalId}") {
        fun createRoute(animalId: Int? = null) =
            if (animalId != null) "formulario_animal?animalId=$animalId"
            else "formulario_animal"
    }
}