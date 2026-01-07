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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganaderia.ganaderiaapp.ui.components.*
import com.ganaderia.ganaderiaapp.ui.theme.GanadoColors
import com.ganaderia.ganaderiaapp.ui.viewmodel.DashboardViewModel
import com.ganaderia.ganaderiaapp.ui.viewmodel.GanadoViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToInventario: () -> Unit
) {
    val context = LocalContext.current
    val factory = remember { GanadoViewModelFactory(context) }
    val viewModel: DashboardViewModel = viewModel(factory = factory)

    val kpis by viewModel.kpis.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
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
                    IconButton(
                        onClick = { viewModel.forzarSincronizacion() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Sync, "Sincronizar", tint = Color.White)
                        }
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading && kpis == null -> {
                    LoadingScreen()
                }

                error != null && kpis == null -> {
                    ErrorScreen(error!!) { viewModel.cargarKPIs() }
                }

                kpis != null -> {
                    val currentKpis = kpis!!

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isSyncing) {
                            item {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = GanadoColors.Primary,
                                    trackColor = GanadoColors.Primary.copy(alpha = 0.2f)
                                )
                            }
                        }

                        item {
                            Text(
                                text = "Resumen General",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = GanadoColors.TextPrimary
                            )
                        }

                        item {
                            KPICard(
                                titulo = "Total de Animales",
                                valor = currentKpis.total_animales.toString(),
                                subtitulo = "En el inventario",
                                color = GanadoColors.Primary
                            )
                        }

                        item {
                            KPICard(
                                titulo = "Peso Promedio",
                                valor = currentKpis.peso_promedio ?: "0 kg",
                                subtitulo = "Del ganado",
                                color = GanadoColors.Secondary
                            )
                        }

                        item {
                            Text(
                                text = "Distribución por Sexo",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = GanadoColors.TextPrimary
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                KPICard(
                                    titulo = "Hembras",
                                    valor = currentKpis.total_hembras ?: "0",
                                    color = GanadoColors.BadgeHembra,
                                    modifier = Modifier.weight(1f)
                                )

                                KPICard(
                                    titulo = "Machos",
                                    valor = currentKpis.total_machos ?: "0",
                                    color = GanadoColors.BadgeMacho,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Text(
                                text = "Estado de Salud",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = GanadoColors.TextPrimary
                            )
                        }

                        item {
                            KPICard(
                                titulo = "En Tratamiento",
                                valor = currentKpis.en_tratamiento ?: "0",
                                subtitulo = "Animales con atención médica",
                                color = GanadoColors.Warning
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }

                else -> {
                    EmptyState(
                        icono = "📊",
                        titulo = "Sin datos disponibles",
                        mensaje = "Sincroniza para cargar el dashboard"
                    )
                }
            }

            if (error != null && kpis != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error ?: "Error de conexión",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}