// ============================================
// Archivo: ui/screens/InventarioScreen.kt
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    onNavigateToDetalle: (Int) -> Unit,
    onNavigateToFormulario: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: InventarioViewModel = viewModel()
) {
    val animales by viewModel.animales.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val busqueda by viewModel.busqueda.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Inventario (${animales.size})",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.cargarAnimales() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GanadoColors.Primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToFormulario,
                containerColor = GanadoColors.Success,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Buscador
            OutlinedTextField(
                value = busqueda,
                onValueChange = { viewModel.buscar(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Buscar por ID...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = GanadoColors.Primary
                    )
                },
                trailingIcon = {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = { viewModel.buscar("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GanadoColors.Primary,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            // Lista de animales
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GanadoColors.Primary)
                }
                error != null -> ErrorScreen(error!!) { viewModel.cargarAnimales() }
                animales.isEmpty() -> EmptyState(
                    icono = "🐄",
                    titulo = "No hay animales registrados",
                    mensaje = "Presiona el botón + para agregar uno"
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(animales.size) { index ->
                        AnimalCard(
                            animal = animales[index],
                            onClick = { onNavigateToDetalle(animales[index].id) }
                        )
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = animal.identificacion,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = GanadoColors.Primary
                )
                BadgeChip(
                    texto = animal.sexo,
                    backgroundColor = if (animal.sexo == "Macho")
                        GanadoColors.BadgeMacho
                    else
                        GanadoColors.BadgeHembra
                )
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
                InfoItem(
                    icono = Icons.Default.MonitorWeight,
                    texto = "${animal.peso_actual ?: "N/A"} kg"
                )
                InfoItem(
                    icono = Icons.Default.CalendarToday,
                    texto = "${animal.edad_meses ?: 0} meses"
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