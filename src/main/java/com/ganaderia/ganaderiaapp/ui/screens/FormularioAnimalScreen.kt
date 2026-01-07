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
import com.ganaderia.ganaderiaapp.data.model.AnimalRequest
import com.ganaderia.ganaderiaapp.ui.theme.GanadoColors
import com.ganaderia.ganaderiaapp.ui.viewmodel.FormularioAnimalViewModel
import com.ganaderia.ganaderiaapp.ui.viewmodel.GanadoViewModelFactory
import com.ganaderia.ganaderiaapp.data.model.Animal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioAnimalScreen(
    animalId: Int?,
    onNavigateBack: () -> Unit,
    viewModel: FormularioAnimalViewModel
) {
    // Estados del ViewModel
    val hembras by viewModel.hembras.collectAsState()
    val machos by viewModel.machos.collectAsState()
    val animalActual by viewModel.animalActual.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Estados del Formulario (Inicializados)
    var identificacion by remember { mutableStateOf("") }
    var razaSeleccionada by remember { mutableStateOf("Brahman") }
    var sexoSeleccionado by remember { mutableStateOf("Macho") }
    var categoriaSeleccionada by remember { mutableStateOf("Ternero/a") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var fechaIngreso by remember { mutableStateOf("") }
    var pesoActual by remember { mutableStateOf("") }
    var estadoSalud by remember { mutableStateOf("Bueno") }
    var madreSeleccionada by remember { mutableStateOf<Int?>(null) }
    var padreSeleccionado by remember { mutableStateOf<Int?>(null) }
    var notas by remember { mutableStateOf("") }

    // Control de UI
    var showDatePickerNac by remember { mutableStateOf(false) }
    var showDatePickerIng by remember { mutableStateOf(false) }
    var mostrarErrores by remember { mutableStateOf(false) }
    var datosYaCargados by remember { mutableStateOf(false) }

    val razas = listOf("Brahman", "Angus", "Hereford", "Charolais", "Simmental", "Holstein", "Jersey", "Nelore", "Gyr", "Senepol", "Brangus", "Santa Gertrudis", "Criollo", "Mestizo", "Otra")
    val categorias = listOf("Ternero/a", "Novillo/a", "Torete", "Vaquilla", "Toro", "Vaca", "Buey")

    // Agrega esto para que la pantalla se cierre al guardar con éxito
    LaunchedEffect(Unit) {
        viewModel.operacionExitosa.collect { exito ->
            if (exito) onNavigateBack()
        }
    }

    // CARGAR DATOS
    LaunchedEffect(animalId) {
        viewModel.cargarOpcionesPadres()
        if (animalId != null) {
            viewModel.cargarAnimal(animalId)
        }
    }

    // MAPEO DE DATOS PARA EDICIÓN
    // MAPEO DE DATOS PARA EDICIÓN
    LaunchedEffect(animalActual) {
        animalActual?.let { animal: Animal -> // Especificar el tipo Animal ayuda al IDE
            if (!datosYaCargados) {
                identificacion = animal.identificacion
                razaSeleccionada = animal.raza
                sexoSeleccionado = animal.sexo
                categoriaSeleccionada = animal.categoria
                // Asegúrate de que estos campos existan en tu data class Animal
                fechaNacimiento = animal.fecha_nacimiento.take(10)
                fechaIngreso = animal.fecha_ingreso.take(10)
                pesoActual = animal.peso_actual ?: ""
                estadoSalud = animal.estado_salud
                notas = animal.notas ?: ""

                // Búsqueda de padres
                madreSeleccionada = hembras.find { it.identificacion == animal.madre_identificacion }?.id
                padreSeleccionado = machos.find { it.identificacion == animal.padre_identificacion }?.id

                datosYaCargados = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (animalId == null) "Nuevo Animal" else "Editar Animal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GanadoColors.Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección: Identificación y Raza
            item {
                OutlinedTextField(
                    value = identificacion,
                    onValueChange = { identificacion = it },
                    label = { Text("Identificación *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = mostrarErrores && identificacion.isEmpty(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                SelectorDropdown(
                    label = "Raza *",
                    opciones = razas,
                    seleccionado = razaSeleccionada,
                    onSeleccion = { razaSeleccionada = it }
                )
            }

            // Sección: Sexo y Categoría
            item {
                Column {
                    Text("Sexo *", style = MaterialTheme.typography.labelLarge, color = GanadoColors.TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Macho", "Hembra").forEach { sexo ->
                            FilterChip(
                                selected = sexoSeleccionado == sexo,
                                onClick = { sexoSeleccionado = sexo },
                                label = { Text(sexo) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                SelectorDropdown(
                    label = "Categoría *",
                    opciones = categorias,
                    seleccionado = categoriaSeleccionada,
                    onSeleccion = { categoriaSeleccionada = it }
                )
            }

            // Sección: Fechas
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = fechaNacimiento, onValueChange = {}, readOnly = true,
                        label = { Text("F. Nacimiento *") },
                        modifier = Modifier.weight(1f),
                        trailingIcon = { IconButton(onClick = { showDatePickerNac = true }) { Icon(Icons.Default.DateRange, null) } },
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = fechaIngreso, onValueChange = {}, readOnly = true,
                        label = { Text("F. Ingreso *") },
                        modifier = Modifier.weight(1f),
                        trailingIcon = { IconButton(onClick = { showDatePickerIng = true }) { Icon(Icons.Default.DateRange, null) } },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Sección: Genealogía
            item {
                SelectorPadres(
                    label = "Madre",
                    opciones = hembras,
                    seleccionadoId = madreSeleccionada,
                    onSeleccion = { madreSeleccionada = it }
                )
            }

            item {
                SelectorPadres(
                    label = "Padre",
                    opciones = machos,
                    seleccionadoId = padreSeleccionado,
                    onSeleccion = { padreSeleccionado = it }
                )
            }

            // Botón Guardar
            item {
                Button(
                    onClick = {
                        if (identificacion.isBlank() || fechaNacimiento.isBlank()) {
                            mostrarErrores = true
                        } else {
                            val request = AnimalRequest(
                                identificacion = identificacion,
                                raza = razaSeleccionada,
                                sexo = sexoSeleccionado,
                                categoria = categoriaSeleccionada,
                                fecha_nacimiento = fechaNacimiento,
                                fecha_ingreso = fechaIngreso,
                                peso_actual = pesoActual.toDoubleOrNull(), // Asegúrate que sea String si así está en tu modelo
                                estado_salud = estadoSalud,
                                madre_id = madreSeleccionada,
                                padre_id = padreSeleccionado,
                                notas = notas.ifBlank { null }
                            )

                            // Usamos la función unificada del ViewModel
                            viewModel.guardarAnimal(request)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GanadoColors.Primary)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text("Guardar Animal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Date Pickers
    if (showDatePickerNac) VentanaFecha({ fechaNacimiento = it }, { showDatePickerNac = false })
    if (showDatePickerIng) VentanaFecha({ fechaIngreso = it }, { showDatePickerIng = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorDropdown(label: String, opciones: List<String>, seleccionado: String, onSeleccion: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        OutlinedTextField(
            value = seleccionado, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandido) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onSeleccion(it); expandido = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorPadres(label: String, opciones: List<com.ganaderia.ganaderiaapp.data.model.AnimalSimple>, seleccionadoId: Int?, onSeleccion: (Int?) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    val textoSeleccionado = opciones.find { it.id == seleccionadoId }?.identificacion ?: "Ninguno"

    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        OutlinedTextField(
            value = textoSeleccionado, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandido) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            DropdownMenuItem(text = { Text("Ninguno") }, onClick = { onSeleccion(null); expandido = false })
            opciones.forEach { animal ->
                DropdownMenuItem(text = { Text(animal.identificacion) }, onClick = { onSeleccion(animal.id); expandido = false })
            }
        }
    }
}