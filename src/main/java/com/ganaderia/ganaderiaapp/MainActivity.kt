package com.ganaderia.ganaderiaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ganaderia.ganaderiaapp.ui.screens.*
import com.ganaderia.ganaderiaapp.ui.theme.GanadoTheme
import com.ganaderia.ganaderiaapp.ui.viewmodel.GanadoViewModelFactory
import com.ganaderia.ganaderiaapp.ui.viewmodel.FormularioAnimalViewModel
import com.ganaderia.ganaderiaapp.data.repository.GanadoRepository
import androidx.compose.ui.platform.LocalContext
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Aquí deberías obtener la instancia de tu repositorio
        // Normalmente se hace a través de un Singleton o una clase Application
        // val repository = (application as GanaderiaApp).repository

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
        // 1. Dashboard
        composable("dashboard") {
            DashboardScreen(
                onNavigateToInventario = {
                    navController.navigate("inventario")
                }
            )
        }

        // 2. Inventario (Lista de Animales)
        composable("inventario") {
            InventarioScreen(
                onNavigateToDetalle = { id ->
                    // Usamos 'id' directamente porque es lo que envía InventarioScreen
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

        // 3. Detalle del Animal
        composable(
            route = "detalle/{animalId}",
            arguments = listOf(
                navArgument("animalId") { type = NavType.IntType } // Esto le dice a Android que convierta el texto a número
            )
        ) { backStackEntry ->
            // Ahora getInt("animalId") funcionará correctamente y no dará error
            val animalId = backStackEntry.arguments?.getInt("animalId") ?: 0

            DetalleAnimalScreen(
                animalId = animalId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditar = { id ->
                    navController.navigate("formulario/$id")
                },
                navController = navController
            )
        }

        // 4. Formulario (Crear Nuevo)
        composable("formulario") {
            val context = LocalContext.current
            val viewModel: FormularioAnimalViewModel = viewModel(
                factory = GanadoViewModelFactory(context)
            )

            FormularioAnimalScreen(
                animalId = null,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5. Formulario (Editar Existente)
        // 5. Formulario (Editar Existente)
        composable(
            route = "formulario/{animalId}",
            arguments = listOf(
                navArgument("animalId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val animalId = backStackEntry.arguments?.getInt("animalId")
            val context = LocalContext.current

            // 1. Necesitas crear el ViewModel aquí también
            val viewModel: FormularioAnimalViewModel = viewModel(
                factory = GanadoViewModelFactory(context)
            )

            // 2. Pásalo a la pantalla
            FormularioAnimalScreen(
                animalId = animalId,
                viewModel = viewModel, // <--- Esto es lo que faltaba
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}