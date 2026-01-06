package com.ganaderia.ganaderiaapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset // IMPORTANTE: Resuelve el error de unresolved
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganaderia.ganaderiaapp.data.model.VacunaRequest
import com.ganaderia.ganaderiaapp.ui.viewmodel.DetalleAnimalViewModel
import com.ganaderia.ganaderiaapp.ui.components.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleAnimalScreen(
    animalId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEditar: (Int) -> Unit,
    viewModel: DetalleAnimalViewModel = viewModel()
) {
    val animal by viewModel.animal.collectAsState()
    val vacunas by viewModel.vacunas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val catalogo by viewModel.catalogoVacunas.collectAsState()

    var tabSeleccionada by remember { mutableIntStateOf(0) }
    var mostrarDialogoVacuna by remember { mutableStateOf(false) }

    LaunchedEffect(animalId) {
        viewModel.cargarAnimal(animalId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Animal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEditar(animalId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF2E7D32))
                    }
                    IconButton(onClick = { /* Lógica borrar */ }) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA))) {
            when {
                isLoading -> LoadingScreen()
                error != null -> ErrorScreen(error!!) { viewModel.cargarAnimal(animalId) }
                animal != null -> {
                    Column {
                        TabRow(
                            selectedTabIndex = tabSeleccionada,
                            containerColor = Color.White,
                            contentColor = Color(0xFF2E7D32),
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[tabSeleccionada]),
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        ) {
                            Tab(selected = tabSeleccionada == 0, onClick = { tabSeleccionada = 0 }, text = { Text("General") })
                            Tab(selected = tabSeleccionada == 1, onClick = { tabSeleccionada = 1 }, text = { Text("Salud") })
                        }

                        when (tabSeleccionada) {
                            0 -> PestañaGeneralCompleta(animal!!)
                            1 -> PestañaSaludEstilizada(vacunas) { mostrarDialogoVacuna = true }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoVacuna) {
        // Obtenemos la lista del catálogo del StateFlow del ViewModel
        val catalogo by viewModel.catalogoVacunas.collectAsState()

        RegistrarVacunaDialog(
            animalId = animalId,
            opcionesCatalogo = catalogo,
            onDismiss = { mostrarDialogoVacuna = false },
            onConfirm = { request ->
                viewModel.registrarVacuna(request) { mostrarDialogoVacuna = false }
            },
            // ESTA ES LA LÍNEA QUE FALTA:
            onNuevaVacunaCatalogo = { nombre ->
                viewModel.agregarAlCatalogo(nombre)
            }
        )
    }
}

@Composable
fun PestañaGeneralCompleta(animal: com.ganaderia.ganaderiaapp.data.model.Animal) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "ID: ${animal.identificacion}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            DetalleCard(titulo = "Información Básica") {
                InfoRow(Icons.Default.Info, "Raza", animal.raza)
                InfoRow(Icons.Default.Person, "Sexo", animal.sexo)
                InfoRow(Icons.Default.Star, "Categoría", animal.categoria)
                InfoRow(Icons.Default.Favorite, "Estado", animal.estado_salud)
            }
        }

        // Usando tus nombres de variables: padre_identificacion y madre_identificacion
        item {
            DetalleCard(titulo = "Genealogía") {
                InfoRow(Icons.Default.Male, "Padre", animal.padre_identificacion ?: "Sin registro")
                InfoRow(Icons.Default.Female, "Madre", animal.madre_identificacion ?: "Sin registro")
            }
        }

        item {
            DetalleCard(titulo = "Producción") {
                InfoRow(Icons.Default.MonitorWeight, "Peso Actual", "${animal.peso_actual ?: "---"} kg")
                InfoRow(Icons.Default.DateRange, "Nacimiento", animal.fecha_nacimiento)
            }
        }
    }
}

@Composable
fun DetalleCard(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 13.sp)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            contenido()
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarVacunaDialog(
    animalId: Int,
    opcionesCatalogo: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (VacunaRequest) -> Unit,
    onNuevaVacunaCatalogo: (String) -> Unit
) {
    // Estados para todos los campos
    var tipoVacuna by remember { mutableStateOf("") }
    var nombreComercial by remember { mutableStateOf("") }
    var fechaAplicacion by remember { mutableStateOf("2026-01-06") }
    var proximaDosis by remember { mutableStateOf("") }
    var dosis by remember { mutableStateOf("") }
    var lote by remember { mutableStateOf("") }
    var veterinario by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    // Estados de control para DatePickers y UI
    var showDatePickerApp by remember { mutableStateOf(false) }
    var showDatePickerProx by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var mostrarDialogoNuevoNombre by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf("") }

    // --- Lógica de DatePickers ---
    if (showDatePickerApp) {
        VentanaFecha(
            onDateSelected = { fechaAplicacion = it },
            onDismiss = { showDatePickerApp = false }
        )
    }

    if (showDatePickerProx) {
        VentanaFecha(
            onDateSelected = { proximaDosis = it },
            onDismiss = { showDatePickerProx = false }
        )
    }

    // --- Diálogo para añadir al catálogo ---
    if (mostrarDialogoNuevoNombre) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoNuevoNombre = false },
            title = { Text("Nueva Vacuna al Catálogo") },
            text = {
                OutlinedTextField(
                    value = nuevoNombre,
                    onValueChange = { nuevoNombre = it },
                    label = { Text("Nombre del tipo de vacuna") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nuevoNombre.isNotBlank()) {
                        onNuevaVacunaCatalogo(nuevoNombre)
                        tipoVacuna = nuevoNombre
                        mostrarDialogoNuevoNombre = false
                        nuevoNombre = ""
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoNuevoNombre = false }) { Text("Cancelar") }
            }
        )
    }

    // --- Diálogo Principal ---
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Registrar Vacuna", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Selector de Tipo de Vacuna
                item {
                    Text("Tipo de Vacuna *", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = tipoVacuna,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Seleccionar tipo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            opcionesCatalogo.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(opcion) },
                                    onClick = { tipoVacuna = opcion; expanded = false }
                                )
                            }
                        }
                    }
                    TextButton(onClick = { mostrarDialogoNuevoNombre = true }) {
                        Text("+ Añadir nueva vacuna al catálogo", color = Color(0xFF1B5E20))
                    }
                }

                // Campos que aparecen al elegir la vacuna
                if (tipoVacuna.isNotBlank()) {
                    item {
                        Text("Nombre Comercial", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        OutlinedTextField(
                            value = nombreComercial,
                            onValueChange = { nombreComercial = it },
                            placeholder = { Text("Ej: Bravoxin") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Fecha Aplicación *", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                OutlinedTextField(
                                    value = fechaAplicacion,
                                    onValueChange = { },
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = { showDatePickerApp = true }) {
                                            Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Próxima Dosis", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                OutlinedTextField(
                                    value = proximaDosis,
                                    onValueChange = { },
                                    readOnly = true,
                                    placeholder = { Text("dd/mm/aaaa") },
                                    trailingIcon = {
                                        IconButton(onClick = { showDatePickerProx = true }) {
                                            Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Dosis", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                OutlinedTextField(
                                    value = dosis,
                                    onValueChange = { dosis = it },
                                    placeholder = { Text("Ej: 2ml") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Lote", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                OutlinedTextField(
                                    value = lote,
                                    onValueChange = { lote = it },
                                    placeholder = { Text("Ej: L12345") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    item {
                        Text("Veterinario", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        OutlinedTextField(
                            value = veterinario,
                            onValueChange = { veterinario = it },
                            placeholder = { Text("Nombre del veterinario") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text("Notas", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        OutlinedTextField(
                            value = notas,
                            onValueChange = { notas = it },
                            placeholder = { Text("Observaciones...") },
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        VacunaRequest(
                            animal_id = animalId,
                            nombre_vacuna = tipoVacuna,
                            fecha_aplicacion = fechaAplicacion,
                            dosis = dosis,
                            lote = lote.ifBlank { null },
                            veterinario = veterinario,
                            proxima_dosis = proximaDosis.ifBlank { null },
                            observaciones = notas.ifBlank { null }
                        )
                    )
                },
                enabled = tipoVacuna.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
            ) {
                Icon(Icons.Default.Done, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Registrar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// Función auxiliar para los diálogos de fecha
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentanaFecha(onDateSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val date = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneId.of("UTC"))
                        .toLocalDate()
                    onDateSelected(date.toString())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MostrarDatePicker(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    // Convertir milisegundos a formato YYYY-MM-DD
                    val date = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    onDateSelected(date.toString())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun PestañaSaludEstilizada(
    vacunas: List<com.ganaderia.ganaderiaapp.data.model.Vacuna>,
    onAgregar: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        // Dentro de PestañaSaludEstilizada
        Button(
            onClick = onAgregar, // Esto debe abrir el diálogo actualizado
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(26.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(" Registrar Vacuna", fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(vacunas) { vacuna ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(vacuna.nombre_vacuna, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Fecha: ${vacuna.fecha_aplicacion.take(10)}") }
                    )
                }
            }
        }
    }
}