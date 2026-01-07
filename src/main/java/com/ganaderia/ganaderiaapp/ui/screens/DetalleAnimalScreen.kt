package com.ganaderia.ganaderiaapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ganaderia.ganaderiaapp.data.model.VacunaRequest
import com.ganaderia.ganaderiaapp.ui.viewmodel.DetalleAnimalViewModel
import com.ganaderia.ganaderiaapp.ui.viewmodel.GanadoViewModelFactory
import com.ganaderia.ganaderiaapp.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleAnimalScreen(
    animalId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEditar: (Int) -> Unit,
    navController: NavController,
) {
    val context = LocalContext.current
    val factory = remember { GanadoViewModelFactory(context) }
    val viewModel: DetalleAnimalViewModel = viewModel(factory = factory)

    val animal by viewModel.animal.collectAsState()
    val vacunas by viewModel.vacunas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val catalogo by viewModel.catalogoVacunas.collectAsState()

    var tabSeleccionada by remember { mutableIntStateOf(0) }
    var mostrarDialogoVacuna by remember { mutableStateOf(false) }
    var mostrarConfirmarBorrado by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { mostrarConfirmarBorrado = true }) {
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
                            1 -> PestañaSaludEstilizada(
                                animalId = animalId,
                                vacunas = vacunas,
                                viewModel = viewModel,
                                onAgregar = { mostrarDialogoVacuna = true }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para borrar animal
    if (mostrarConfirmarBorrado) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarBorrado = false },
            title = { Text("¿Eliminar Animal?") },
            text = { Text("Se eliminará permanentemente a ${animal?.identificacion}. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        animal?.id?.let { id ->
                            viewModel.eliminarAnimal(id) {
                                navController.popBackStack()
                            }
                        }
                        mostrarConfirmarBorrado = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmarBorrado = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarDialogoVacuna) {
        RegistrarVacunaDialog(
            animalId = animalId,
            opcionesCatalogo = catalogo,
            onDismiss = { mostrarDialogoVacuna = false },
            onConfirm = { request ->
                viewModel.registrarVacuna(request) {
                    mostrarDialogoVacuna = false
                }
            },
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ID: ${animal.identificacion}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (!animal.sincronizado) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "No sincronizado",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        item {
            DetalleCard(titulo = "Información Básica") {
                InfoRow(Icons.Default.Info, "Raza", animal.raza)
                InfoRow(Icons.Default.Person, "Sexo", animal.sexo)
                InfoRow(Icons.Default.Star, "Categoría", animal.categoria)
                InfoRow(Icons.Default.Favorite, "Estado", animal.estado_salud)
            }
        }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentanaFecha(onDateSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    // Compatible con API 24 usando java.util.Calendar
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    calendar.timeInMillis = millis
                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                    onDateSelected(formatter.format(calendar.time))
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) {
        DatePicker(state = datePickerState)
    }
}

// El resto de componentes (DetalleCard, InfoRow, RegistrarVacunaDialog, PestañaSaludEstilizada)
// se mantienen igual que en tu código original, asegurando que PestañaSaludEstilizada
// use el viewModel.eliminarVacuna(v.id, animalId).

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
    var tipoVacuna by remember { mutableStateOf("") }
    var fechaAplicacion by remember { mutableStateOf("2026-01-06") }
    var proximaDosis by remember { mutableStateOf("") }
    var dosis by remember { mutableStateOf("") }
    var lote by remember { mutableStateOf("") }
    var veterinario by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    var showDatePickerApp by remember { mutableStateOf(false) }
    var showDatePickerProx by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var mostrarDialogoNuevoNombre by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf("") }

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

                if (tipoVacuna.isNotBlank()) {
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
                            dosis = dosis.ifBlank { null },
                            lote = lote.ifBlank { null },
                            veterinario = veterinario.ifBlank { null },
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

@Composable
fun PestañaSaludEstilizada(
    animalId: Int,
    vacunas: List<com.ganaderia.ganaderiaapp.data.model.Vacuna>,
    viewModel: DetalleAnimalViewModel,
    onAgregar: () -> Unit
) {
    var vacunaParaBorrar by remember { mutableStateOf<com.ganaderia.ganaderiaapp.data.model.Vacuna?>(null) }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Button(
            onClick = onAgregar,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(26.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(" Registrar Vacuna", fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        if (vacunas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay vacunas registradas", color = Color.Gray)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(vacunas) { vacuna ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(vacuna.nombre_vacuna, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Column {
                                Text("Fecha: ${vacuna.fecha_aplicacion.take(10)}")
                                if (!vacuna.proxima_dosis.isNullOrBlank()) {
                                    Text("Próxima: ${vacuna.proxima_dosis.take(10)}", color = Color(0xFFD32F2F))
                                }
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { vacunaParaBorrar = vacuna }) {
                                Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color(0xFFC62828))
                            }
                        },
                        leadingContent = {
                            Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Color(0xFF2E7D32))
                        }
                    )
                }
            }
        }
    }

    if (vacunaParaBorrar != null) {
        AlertDialog(
            onDismissRequest = { vacunaParaBorrar = null },
            title = { Text("¿Eliminar Vacuna?") },
            text = { Text("Se eliminará el registro de ${vacunaParaBorrar?.nombre_vacuna}. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vacunaParaBorrar?.let { v ->
                            viewModel.eliminarVacuna(v.id, animalId)
                        }
                        vacunaParaBorrar = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { vacunaParaBorrar = null }) { Text("Cancelar") }
            }
        )
    }
}