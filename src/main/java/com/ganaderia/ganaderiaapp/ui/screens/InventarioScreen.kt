// ============================================
// InventarioScreen.kt - Sincronización automática
// ============================================
package com.ganaderia.ganaderiaapp.ui.screens

import androidx.compose.foundation.clickable
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
import com.ganaderia.ganaderiaapp.data.model.Animal
import com.ganaderia.ganaderiaapp.ui.components.*
import com.ganaderia.ganaderiaapp.ui.theme.GanadoColors
import com.ganaderia.ganaderiaapp.ui.viewmodel.InventarioViewModel
import com.ganaderia.ganaderiaapp.ui.viewmodel.GanadoViewModelFactory
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    onNavigateToDetalle: (Int) -> Unit,
    onNavigateToFormulario: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: InventarioViewModel = viewModel(
        factory = GanadoViewModelFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val animales by viewModel.animales.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val error by viewModel.error.collectAsState()

    val animalesNoSincronizados = animales.count { !it.sincronizado }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Inventario de Ganado", fontWeight = FontWeight.Bold)
                        if (animalesNoSincronizados > 0) {
                            Text(
                                "$animalesNoSincronizados pendientes (auto-sync)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        } else if (isSyncing) {
                            Text(
                                "Sincronizando...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Atrás", tint = Color.White)
                    }
                },
                actions = {
                    // 🔧 NUEVO: Indicador visual de sincronización
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }

                    // Botón de refresh manual (opcional)
                    IconButton(onClick = { viewModel.refrescar() }) {
                        Icon(Icons.Default.Refresh, "Actualizar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GanadoColors.Primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToFormulario,
                containerColor = GanadoColors.Primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nuevo Animal")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 🔧 NUEVO: Indicador sutil de sincronización
            if (isSyncing || animalesNoSincronizados > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    color = if (isSyncing) GanadoColors.Info.copy(alpha = 0.1f) else Color(0xFFFF9800).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = GanadoColors.Info,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isSyncing)
                                "Sincronizando automáticamente..."
                            else
                                "$animalesNoSincronizados cambios se sincronizarán automáticamente",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSyncing) GanadoColors.Info else Color(0xFFFF9800)
                        )
                    }
                }
            }

            if (error != null && animales.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sin conexión - Datos guardados localmente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            when {
                error != null && animales.isEmpty() -> {
                    ErrorScreen(error!!) { viewModel.refrescar() }
                }

                animales.isEmpty() && !isLoading -> {
                    EmptyState(
                        icono = "🐄",
                        titulo = "No hay animales registrados",
                        mensaje = "Presiona el botón + para agregar tu primer animal"
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(animales) { animal ->
                            AnimalCard(
                                animal = animal,
                                onClick = { onNavigateToDetalle(animal.localId) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimalCard(
    animal: Animal,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = animal.identificacion,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = GanadoColors.Primary
                    )
                    Text(
                        text = "Local ID: ${animal.localId} | Server ID: ${if(animal.id == 0) "Pendiente" else animal.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (animal.sincronizado) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                        contentDescription = if (animal.sincronizado) "Sincronizado" else "Pendiente",
                        tint = if (animal.sincronizado) GanadoColors.Success else Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )

                    BadgeChip(
                        texto = animal.sexo,
                        backgroundColor = if (animal.sexo == "Macho")
                            GanadoColors.BadgeMacho
                        else
                            GanadoColors.BadgeHembra
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BadgeChip(
                    texto = animal.raza,
                    backgroundColor = GanadoColors.BadgeRaza
                )
                BadgeChip(
                    texto = animal.categoria,
                    backgroundColor = GanadoColors.Secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val pesoFormateado = animal.peso_actual?.toDoubleOrNull()?.let {
                    "%.2f kg".format(it)
                } ?: "--- kg"

                InfoItem(
                    icono = Icons.Default.MonitorWeight,
                    texto = pesoFormateado
                )
                InfoItem(
                    icono = Icons.Default.CalendarToday,
                    texto = "${animal.edad_meses} meses"
                )
                InfoItem(
                    icono = Icons.Default.HealthAndSafety,
                    texto = animal.estado_salud
                )
            }
        }
    }
}

@Composable
fun InfoItem(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icono,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = GanadoColors.TextSecondary
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = GanadoColors.TextSecondary
        )
    }
}