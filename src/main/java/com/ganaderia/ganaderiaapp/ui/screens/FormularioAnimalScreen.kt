// ============================================
// Archivo: ui/screens/FormularioAnimalScreen.kt
// ============================================
package com.ganaderia.ganaderiaapp.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ganaderia.ganaderiaapp.data.model.AnimalRequest
import com.ganaderia.ganaderiaapp.ui.theme.GanadoColors
import com.ganaderia.ganaderiaapp.ui.viewmodel.FormularioAnimalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioAnimalScreen(
    animalId: Int? = null,
    onNavigateBack: () -> Unit,
    viewModel: FormularioAnimalViewModel = viewModel()
) {
    val hembras by viewModel.hembras.collectAsState()
    val machos by viewModel.machos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

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

    var mostrarErrores by remember { mutableStateOf(false) }

    val razas = listOf(
        "Brahman", "Angus", "Hereford", "Charolais", "Simmental",
        "Holstein", "Jersey", "Nelore", "Gyr", "Senepol",
        "Brangus", "Santa Gertrudis", "Criollo", "Mestizo", "Otra"
    )

    val categorias = listOf(
        "Ternero/a", "Novillo/a", "Torete", "Vaquilla", "Toro", "Vaca", "Buey"
    )

    val estadosSalud = listOf("Excelente", "Bueno", "Regular", "En Tratamiento")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (animalId == null) "Nuevo Animal" else "Editar Animal",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GanadoColors.Primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Identificación
            item {
                OutlinedTextField(
                    value = identificacion,
                    onValueChange = { identificacion = it },
                    label = { Text("Identificación *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = mostrarErrores && identificacion.isEmpty(),
                    supportingText = {
                        if (mostrarErrores && identificacion.isEmpty()) {
                            Text("Campo requerido", color = GanadoColors.Error)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Raza
            item {
                var expandido by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandido,
                    onExpandedChange = { expandido = it }
                ) {
                    OutlinedTextField(
                        value = razaSeleccionada,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Raza *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandido) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandido,
                        onDismissRequest = { expandido = false }
                    ) {
                        razas.forEach { raza ->
                            DropdownMenuItem(
                                text = { Text(raza) },
                                onClick = {
                                    razaSeleccionada = raza
                                    expandido = false
                                }
                            )
                        }
                    }
                }
            }

            // Sexo
            item {
                Column {
                    Text(
                        text = "Sexo *",
                        style = MaterialTheme.typography.labelLarge,
                        color = GanadoColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = sexoSeleccionado == "Macho",
                            onClick = { sexoSeleccionado = "Macho" },
                            label = { Text("Macho") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                if (sexoSeleccionado == "Macho") {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GanadoColors.BadgeMacho.copy(alpha = 0.3f)
                            )
                        )
                        FilterChip(
                            selected = sexoSeleccionado == "Hembra",
                            onClick = { sexoSeleccionado = "Hembra" },
                            label = { Text("Hembra") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                if (sexoSeleccionado == "Hembra") {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GanadoColors.BadgeHembra.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Categoría
            item {
                var expandido by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandido,
                    onExpandedChange = { expandido = it }
                ) {
                    OutlinedTextField(
                        value = categoriaSeleccionada,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandido) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandido,
                        onDismissRequest = { expandido = false }
                    ) {
                        categorias.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria) },
                                onClick = {
                                    categoriaSeleccionada = categoria
                                    expandido = false
                                }
                            )
                        }
                    }
                }
            }

            // Fechas
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = fechaNacimiento,
                        onValueChange = { fechaNacimiento = it },
                        label = { Text("F. Nacimiento *") },
                        placeholder = { Text("AAAA-MM-DD") },
                        modifier = Modifier.weight(1f),
                        isError = mostrarErrores && fechaNacimiento.isEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = fechaIngreso,
                        onValueChange = { fechaIngreso = it },
                        label = { Text("F. Ingreso *") },
                        placeholder = { Text("AAAA-MM-DD") },
                        modifier = Modifier.weight(1f),
                        isError = mostrarErrores && fechaIngreso.isEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Peso y Salud
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = pesoActual,
                        onValueChange = { pesoActual = it },
                        label = { Text("Peso (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    var expandido by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandido,
                        onExpandedChange = { expandido = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = estadoSalud,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Estado Salud") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandido) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandido,
                            onDismissRequest = { expandido = false }
                        ) {
                            estadosSalud.forEach { estado ->
                                DropdownMenuItem(
                                    text = { Text(estado) },
                                    onClick = {
                                        estadoSalud = estado
                                        expandido = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Madre
            item {
                var expandido by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandido,
                    onExpandedChange = { expandido = it }
                ) {
                    OutlinedTextField(
                        value = hembras.find { it.id == madreSeleccionada }?.identificacion ?: "Ninguna",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Madre (solo hembras)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandido) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandido,
                        onDismissRequest = { expandido = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ninguna") },
                            onClick = {
                                madreSeleccionada = null
                                expandido = false
                            }
                        )
                        hembras.forEach { hembra ->
                            DropdownMenuItem(
                                text = { Text("${hembra.identificacion} (${hembra.raza})") },
                                onClick = {
                                    madreSeleccionada = hembra.id
                                    expandido = false
                                }
                            )
                        }
                    }
                }
            }

            // Padre
            item {
                var expandido by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandido,
                    onExpandedChange = { expandido = it }
                ) {
                    OutlinedTextField(
                        value = machos.find { it.id == padreSeleccionado }?.identificacion ?: "Ninguno",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Padre (solo machos)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandido) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandido,
                        onDismissRequest = { expandido = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ninguno") },
                            onClick = {
                                padreSeleccionado = null
                                expandido = false
                            }
                        )
                        machos.forEach { macho ->
                            DropdownMenuItem(
                                text = { Text("${macho.identificacion} (${macho.raza})") },
                                onClick = {
                                    padreSeleccionado = macho.id
                                    expandido = false
                                }
                            )
                        }
                    }
                }
            }

            // Notas
            item {
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Error
            if (error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = GanadoColors.Error.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = error!!,
                            modifier = Modifier.padding(16.dp),
                            color = GanadoColors.Error
                        )
                    }
                }
            }

            // Botón guardar
            item {
                Button(
                    onClick = {
                        if (identificacion.isEmpty() || fechaNacimiento.isEmpty() || fechaIngreso.isEmpty()) {
                            mostrarErrores = true
                        } else {
                            val animal = AnimalRequest(
                                identificacion = identificacion,
                                raza = razaSeleccionada,
                                sexo = sexoSeleccionado,
                                categoria = categoriaSeleccionada,
                                fecha_nacimiento = fechaNacimiento,
                                fecha_ingreso = fechaIngreso,
                                peso_actual = pesoActual.toDoubleOrNull(),
                                estado_salud = estadoSalud,
                                madre_id = madreSeleccionada,
                                padre_id = padreSeleccionado,
                                notas = notas.ifEmpty { null }
                            )

                            if (animalId == null) {
                                viewModel.crearAnimal(animal) { onNavigateBack() }
                            } else {
                                viewModel.actualizarAnimal(animalId, animal) { onNavigateBack() }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GanadoColors.Primary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Animal", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}