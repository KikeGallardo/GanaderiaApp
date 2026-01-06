// ============================================
// Archivo: ui/screens/DashboardScreen.kt
// ============================================
package com.ganaderia.ganaderiaapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganaderia.ganaderiaapp.ui.components.*
import com.ganaderia.ganaderiaapp.ui.theme.GanadoColors
import com.ganaderia.ganaderiaapp.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToInventario: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val kpis by viewModel.kpis.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Ganadero", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GanadoColors.Primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.cargarKPIs() }) {
                        Icon(Icons.Default.Refresh, "Actualizar", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToInventario,
                containerColor = GanadoColors.Primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.List, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver Inventario")
            }
        }
    ) { padding ->
        when {
            isLoading -> LoadingScreen()
            error != null -> ErrorScreen(error!!) { viewModel.cargarKPIs() }
            kpis != null -> {
                // --- PREPARACIÓN DE DATOS ---
                val total = kpis?.total_animales ?: 0
                val hembrasCount = kpis?.total_hembras?.toIntOrNull() ?: 0
                val machosCount = kpis?.total_machos?.toIntOrNull() ?: 0

                // Convertimos el peso de String a Double para el formateo
                val pesoNumeric = kpis?.peso_promedio?.toDoubleOrNull() ?: 0.0

                val porcHembras = if (total > 0) (hembrasCount.toDouble() / total) * 100 else 0.0
                val porcMachos = if (total > 0) 100.0 - porcHembras else 0.0

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Resumen General",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Fila 1: Totales
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            KPICard(
                                titulo = "Total Animales",
                                valor = "$total",
                                color = GanadoColors.Primary,
                                modifier = Modifier.weight(1f)
                            )
                            KPICard(
                                titulo = "En Tratamiento",
                                valor = "${kpis?.en_tratamiento ?: "0"}",
                                color = GanadoColors.Warning,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Peso Promedio Corregido
                    item {
                        KPICard(
                            titulo = "Peso Promedio",
                            valor = "${String.format("%.2f", pesoNumeric)} kg",
                            color = GanadoColors.Secondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Fila 2: Hembras y Machos
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            KPICard(
                                titulo = "Hembras",
                                valor = "$hembrasCount",
                                subtitulo = "${String.format("%.1f", porcHembras)}%",
                                color = GanadoColors.BadgeHembra,
                                modifier = Modifier.weight(1f)
                            )
                            KPICard(
                                titulo = "Machos",
                                valor = "$machosCount",
                                subtitulo = "${String.format("%.1f", porcMachos)}%",
                                color = GanadoColors.BadgeMacho,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Info Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = GanadoColors.Primary.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = GanadoColors.Primary, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Sistema de Gestión", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = GanadoColors.Primary)
                                    Text("Controla tu ganado de forma eficiente", style = MaterialTheme.typography.bodySmall, color = GanadoColors.TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}