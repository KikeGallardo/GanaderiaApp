// ============================================
// Archivo: MainActivity.kt (en el paquete raíz)
// ============================================
package com.ganaderia.ganaderiaapp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ganaderia.ganaderiaapp.ui.screens.*
import com.ganaderia.ganaderiaapp.ui.theme.GanadoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GanadoTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        // Dashboard (pantalla principal)
        composable("dashboard") {
            DashboardScreen(
                onNavigateToInventario = {
                    navController.navigate("inventario")
                }
            )
        }

        // Inventario (lista de animales)
        composable("inventario") {
            InventarioScreen(
                onNavigateToDetalle = { id ->
                    navController.navigate("detalle/$id")
                },
                onNavigateToFormulario = {
                    navController.navigate("formulario")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Detalle de un animal específico
        composable(
            route = "detalle/{animalId}",
            arguments = listOf(
                navArgument("animalId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val animalId = backStackEntry.arguments?.getInt("animalId") ?: 0
            DetalleAnimalScreen(
                animalId = animalId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditar = { id ->
                    navController.navigate("formulario/$id")
                }
            )
        }

        // Formulario para crear nuevo animal
        composable("formulario") {
            FormularioAnimalScreen(
                animalId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Formulario para editar animal existente
        composable(
            route = "formulario/{animalId}",
            arguments = listOf(
                navArgument("animalId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val animalId = backStackEntry.arguments?.getInt("animalId")
            FormularioAnimalScreen(
                animalId = animalId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/*
 * FLUJO DE NAVEGACIÓN:
 *
 * dashboard → inventario → detalle/{id} → formulario/{id}
 *                       ↘ formulario (crear nuevo)
 *
 * - Dashboard: Pantalla principal con KPIs
 * - Inventario: Lista de todos los animales
 * - Detalle: Información completa del animal (3 tabs)
 * - Formulario: Crear o editar animal
 */