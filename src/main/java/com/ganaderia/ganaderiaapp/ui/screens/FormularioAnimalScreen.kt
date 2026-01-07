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
import com.ganaderia.ganaderiaapp.data.model.AnimalRequest
import com.ganaderia.ganaderiaapp.ui.theme.GanadoColors
import com.ganaderia.ganaderiaapp.ui.viewmodel.FormularioAnimalViewModel
import android.util.Log
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioAnimalScreen(
    animalId: Int?,
    onNavigateBack: () -> Unit,
    viewModel: FormularioAnimalViewModel
) {
    val hembras by viewModel.hembras.collectAsState()
    val machos by viewModel.machos.collectAsState()
    val animalActual by viewModel.animalActual.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var identificacion by remember { mutableStateOf("") }
    var razaSeleccionada by remember { mutableStateOf("Brahman") }
    var sexoSeleccionado by remember { mutableStateOf("Macho") }
    var categoriaSeleccionada by remember { mutableStateOf("Ternero/a") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var pesoActual by remember { mutableStateOf("") }
    var estadoSalud by remember { mutableStateOf("Bueno") }

    // 🔧 CORREGIDO: Ahora son Strings
    var madreSeleccionada by remember { mutableStateOf<String?>(null) }
    var padreSeleccionado by remember { mutableStateOf<String?>(null) }

    var notas by remember { mutableStateOf("") }

    var showDatePickerNac by remember { mutableStateOf(false) }
    var mostrarErrores by remember { mutableStateOf(false) }
    var datosYaCargados by remember { mutableStateOf(false) }

    val razas = listOf("Brahman", "Angus", "Hereford", "Charolais", "Simmental", "Holstein", "Jersey", "Nelore", "Gyr", "Senepol", "Brangus", "Santa Gertrudis", "Criollo", "Mestizo", "Otra")
    val categorias = listOf("Ternero/a", "Novillo/a", "Torete", "Vaquilla", "Toro", "Vaca", "Buey")

    LaunchedEffect(Unit) {
        viewModel.operacionExitosa.collect { exito ->
            if (exito) onNavigateBack()
        }
    }

    LaunchedEffect(animalId) {
        viewModel.cargarOpcionesPadres()
        if (animalId != null) {
            viewModel.cargarAnimal(animalId)
        }
    }

    LaunchedEffect(animalActual) {
        animalActual?.let { animal ->
            if (!datosYaCargados) {
                identificacion = animal.identificacion
                razaSeleccionada = animal.raza
                sexoSeleccionado = animal.sexo
                categoriaSeleccionada = animal.categoria
                fechaNacimiento = animal.fecha_nacimiento.take(10)
                pesoActual = animal.peso_actual?.toString() ?: ""
                estadoSalud = animal.estado_salud
                notas = animal.notas ?: ""

                // 🔧 CORREGIDO: Asignar identificaciones de texto
                madreSeleccionada = animal.madre_identificacion
                padreSeleccionado = animal.padre_identificacion

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
            item {
                OutlinedTextField(
                    value = identificacion,
                    onValueChange = { identificacion = it },
                    label = { Text("Identificación *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = mostrarErrores && identificacion.isEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        if (mostrarErrores && identificacion.isEmpty()) {
                            Text("Campo obligatorio", color = GanadoColors.Error)
                        }
                    }
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

            item {
                OutlinedTextField(
                    value = fechaNacimiento,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("F. Nacimiento (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = { showDatePickerNac = true }) { Icon(Icons.Default.DateRange, null) } },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = pesoActual,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) pesoActual = it },
                    label = { Text("Peso Actual (kg) *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = mostrarErrores && pesoActual.isEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.MonitorWeight, null) },
                    supportingText = {
                        if (mostrarErrores && pesoActual.isEmpty()) {
                            Text("El peso es obligatorio", color = GanadoColors.Error)
                        }
                    }
                )
            }

            item {
                SelectorDropdown(
                    label = "Estado de Salud *",
                    opciones = listOf("Excelente", "Bueno", "Regular", "En Tratamiento"),
                    seleccionado = estadoSalud,
                    onSeleccion = { estadoSalud = it }
                )
            }

            // 🔧 CORREGIDO: Ahora usa las listas de Strings
            item {
                SelectorPadres(
                    label = "Madre",
                    opciones = hembras,
                    seleccionado = madreSeleccionada,
                    onSeleccion = { madreSeleccionada = it }
                )
            }

            item {
                SelectorPadres(
                    label = "Padre",
                    opciones = machos,
                    seleccionado = padreSeleccionado,
                    onSeleccion = { padreSeleccionado = it }
                )
            }

            item {
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5
                )
            }

            item {
                Button(
                    onClick = {
                        if (identificacion.isBlank() || pesoActual.isBlank()) {
                            mostrarErrores = true
                        } else {
                            val request = AnimalRequest(
                                identificacion = identificacion,
                                raza = razaSeleccionada,
                                sexo = sexoSeleccionado,
                                categoria = categoriaSeleccionada,
                                fecha_nacimiento = if (fechaNacimiento.isBlank()) "2000-01-01" else fechaNacimiento,
                                fecha_ingreso = LocalDate.now().toString(),
                                peso_actual = pesoActual.toDoubleOrNull(),
                                estado_salud = estadoSalud,
                                // 🔧 CORREGIDO: Enviamos strings
                                madre_identificacion = madreSeleccionada,
                                padre_identificacion = padreSeleccionado,
                                notas = notas.ifBlank { null }
                            )
                            viewModel.guardarAnimal(request)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GanadoColors.Primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Done, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (animalId == null) "Registrar Animal" else "Guardar Cambios", fontWeight = FontWeight.Bold)
                    }
                }
            }
            // ... resto del código de error ...
        }
    }

    if (showDatePickerNac) VentanaFecha({ fechaNacimiento = it }, { showDatePickerNac = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorDropdown(label: String, opciones: List<String>, seleccionado: String, onSeleccion: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        OutlinedTextField(
            value = seleccionado,
            onValueChange = {},
            readOnly = true,
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

// 🔧 COMPONENTE ACTUALIZADO PARA STRINGS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorPadres(
    label: String,
    opciones: List<String>,
    seleccionado: String?,
    onSeleccion: (String?) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    val textoMostrar = seleccionado ?: "Ninguno"

    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        OutlinedTextField(
            value = textoMostrar,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandido) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            DropdownMenuItem(
                text = { Text("Ninguno") },
                onClick = { onSeleccion(null); expandido = false }
            )
            opciones.forEach { nombre ->
                DropdownMenuItem(
                    text = { Text(nombre) },
                    onClick = { onSeleccion(nombre); expandido = false }
                )
            }
        }
    }
}