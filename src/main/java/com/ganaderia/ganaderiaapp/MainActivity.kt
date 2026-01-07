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
import androidx.compose.ui.platform.LocalContext

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
        composable("dashboard") {
            DashboardScreen(
                onNavigateToInventario = {
                    navController.navigate("inventario")
                }
            )
        }

        composable("inventario") {
            InventarioScreen(
                onNavigateToDetalle = { localId ->
                    navController.navigate("detalle/$localId")
                },
                onNavigateToFormulario = {
                    navController.navigate("formulario")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "detalle/{localId}",
            arguments = listOf(
                navArgument("localId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val localId = backStackEntry.arguments?.getInt("localId") ?: 0

            DetalleAnimalScreen(
                localId = localId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditar = { id ->
                    navController.navigate("formulario/$id")
                },
                navController = navController
            )
        }

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

        composable(
            route = "formulario/{localId}",
            arguments = listOf(
                navArgument("localId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val localId = backStackEntry.arguments?.getInt("localId")
            val context = LocalContext.current

            val viewModel: FormularioAnimalViewModel = viewModel(
                factory = GanadoViewModelFactory(context)
            )

            FormularioAnimalScreen(
                animalId = localId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}