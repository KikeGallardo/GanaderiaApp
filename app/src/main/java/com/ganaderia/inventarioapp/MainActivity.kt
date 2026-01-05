package com.ganaderia.inventarioapp

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnAgregarAnimal: Button
    private lateinit var llListaAnimales: LinearLayout
    private lateinit var spinnerFiltroSexo: Spinner
    private lateinit var spinnerFiltroRaza: Spinner
    private lateinit var etBuscar: EditText
    private lateinit var tvTotal: TextView
    private lateinit var tvTotalAnimales: TextView
    private lateinit var tvTotalMachos: TextView
    private lateinit var tvTotalHembras: TextView
    private lateinit var cardUltimoAnimal: MaterialCardView
    private lateinit var tvUltimoAnimal: TextView
    private lateinit var cardVacunas: MaterialCardView
    private lateinit var llVacunasProximas: LinearLayout
    private var listaAnimales = listOf<Animal>()
    private lateinit var repository: AnimalRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = AnimalRepository(this)

        inicializarVistas()
        observarAnimales()
        configurarFiltros()

        btnAgregarAnimal.setOnClickListener {
            mostrarDialogoAnimal(null)
        }

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                actualizarLista()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        observarAnimales()
    }

    private fun observarAnimales() {
        lifecycleScope.launch {
            repository.getAllAnimales().collect { lista ->
                listaAnimales.clear()
                listaAnimales.addAll(lista)
                actualizarResumen()
                actualizarLista()
            }
        }
    }

    private fun List<Animal>.addAll(lista: List<Animal>) {
        listaAnimales.addAll(lista)
    }

    private fun List<Animal>.clear() {
        listaAnimales.clear()
    }

    private fun actualizarResumen() {
        tvTotalAnimales.text = listaAnimales.size.toString()
        tvTotalMachos.text = listaAnimales.count { it.sexo == "Macho" }.toString()
        tvTotalHembras.text = listaAnimales.count { it.sexo == "Hembra" }.toString()

        // Último animal añadido
        val ultimoAnimal = listaAnimales.maxByOrNull { it.id }
        if (ultimoAnimal != null) {
            cardUltimoAnimal.visibility = android.view.View.VISIBLE
            tvUltimoAnimal.text = """
                ${ultimoAnimal.identificacion}
                ${ultimoAnimal.raza} • ${ultimoAnimal.sexo} • ${ultimoAnimal.categoria}
                Ubicación: ${ultimoAnimal.potreroUbicacion}
            """.trimIndent()
        } else {
            cardUltimoAnimal.visibility = android.view.View.GONE
        }

        // Vacunas próximas
        mostrarVacunasProximas()
    }

    private fun mostrarVacunasProximas() {
        llVacunasProximas.removeAllViews()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val hoy = Calendar.getInstance()
        val treintaDias = Calendar.getInstance()
        treintaDias.add(Calendar.DAY_OF_YEAR, 30)

        val vacunasProximas = mutableListOf<Pair<Animal, Vacuna>>()

        listaAnimales.forEach { animal ->
            animal.vacunas.forEach { vacuna ->
                try {
                    if (vacuna.proximaDosis.isNotEmpty()) {
                        val fechaVacuna = dateFormat.parse(vacuna.proximaDosis)
                        val calVacuna = Calendar.getInstance()
                        calVacuna.time = fechaVacuna

                        if (calVacuna.after(hoy) && calVacuna.before(treintaDias)) {
                            vacunasProximas.add(Pair(animal, vacuna))
                        }
                    }
                } catch (e: Exception) {
                    // Fecha inválida
                }
            }
        }

        if (vacunasProximas.isNotEmpty()) {
            cardVacunas.visibility = android.view.View.VISIBLE
            vacunasProximas.sortedBy {
                try {
                    dateFormat.parse(it.second.proximaDosis)
                } catch (e: Exception) {
                    Date(Long.MAX_VALUE)
                }
            }.take(5).forEach { (animal, vacuna) ->
                val tv = TextView(this)
                tv.text = "• ${animal.identificacion}: ${vacuna.nombre} - ${vacuna.proximaDosis}"
                tv.textSize = 14f
                tv.setTextColor(resources.getColor(R.color.text_secondary, null))
                tv.setPadding(0, 4, 0, 4)
                llVacunasProximas.addView(tv)
            }
        } else {
            cardVacunas.visibility = android.view.View.GONE
        }
    }

    private fun inicializarVistas() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        btnAgregarAnimal = findViewById(R.id.btnAgregarAnimal)
        llListaAnimales = findViewById(R.id.llListaAnimales)
        spinnerFiltroSexo = findViewById(R.id.spinnerFiltroSexo)
        spinnerFiltroRaza = findViewById(R.id.spinnerFiltroRaza)
        etBuscar = findViewById(R.id.etBuscar)
        tvTotal = findViewById(R.id.tvTotal)
        tvTotalAnimales = findViewById(R.id.tvTotalAnimales)
        tvTotalMachos = findViewById(R.id.tvTotalMachos)
        tvTotalHembras = findViewById(R.id.tvTotalHembras)
        cardUltimoAnimal = findViewById(R.id.cardUltimoAnimal)
        tvUltimoAnimal = findViewById(R.id.tvUltimoAnimal)
        cardVacunas = findViewById(R.id.cardVacunas)
        llVacunasProximas = findViewById(R.id.llVacunasProximas)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_upload -> {
                    subirCambios()
                    true
                }
                R.id.action_download -> {
                    descargarDatos()
                    true
                }
                R.id.action_papelera -> {
                    abrirPapelera()
                    true
                }
                else -> false
            }
        }
    }

    private fun subirCambios() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Subir Cambios")
            .setMessage("¿Deseas subir todos los cambios locales al servidor?")
            .setPositiveButton("Subir") { _, _ ->
                lifecycleScope.launch {
                    val progressDialog = crearDialogoProgreso("Subiendo cambios...")
                    progressDialog.show()

                    val result = repository.subirCambios()

                    progressDialog.dismiss()

                    result.onSuccess { mensaje ->
                        mostrarMensaje(mensaje, true)
                    }.onFailure { error ->
                        mostrarMensaje("Error: ${error.message}", false)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun descargarDatos() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Descargar Datos")
            .setMessage("ADVERTENCIA: Esto reemplazará todos los datos locales con los datos del servidor. ¿Continuar?")
            .setPositiveButton("Descargar") { _, _ ->
                lifecycleScope.launch {
                    val progressDialog = crearDialogoProgreso("Descargando datos...")
                    progressDialog.show()

                    val result = repository.descargarDatos()

                    progressDialog.dismiss()

                    result.onSuccess { mensaje ->
                        mostrarMensaje(mensaje, true)
                    }.onFailure { error ->
                        mostrarMensaje("Error: ${error.message}", false)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearDialogoProgreso(mensaje: String): AlertDialog {
        val builder = MaterialAlertDialogBuilder(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)
        layout.gravity = Gravity.CENTER

        val progressBar = ProgressBar(this)
        val textView = TextView(this)
        textView.text = mensaje
        textView.setPadding(0, 20, 0, 0)

        layout.addView(progressBar)
        layout.addView(textView)

        builder.setView(layout)
        builder.setCancelable(false)

        return builder.create()
    }

    private fun mostrarMensaje(mensaje: String, esExito: Boolean) {
        Snackbar.make(
            findViewById(android.R.id.content),
            mensaje,
            Snackbar.LENGTH_LONG
        ).apply {
            setBackgroundTint(
                if (esExito) resources.getColor(R.color.success, null)
                else resources.getColor(R.color.error, null)
            )
            show()
        }
    }

    private fun abrirPapelera() {
        lifecycleScope.launch {
            val animalesEliminados = repository.getAnimalesEliminados()

            if (animalesEliminados.isEmpty()) {
                Toast.makeText(this@MainActivity, "La papelera está vacía", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val opciones = animalesEliminados.map { "${it.identificacion} (ID: ${it.id})" }

            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("Papelera de Reciclaje (${animalesEliminados.size})")
                .setItems(opciones.toTypedArray()) { _, which ->
                    mostrarOpcionesPapelera(animalesEliminados[which])
                }
                .setNeutralButton("Vaciar Papelera") { _, _ ->
                    vaciarPapelera()
                }
                .setNegativeButton("Cerrar", null)
                .show()
        }
    }

    private fun mostrarOpcionesPapelera(animal: Animal) {
        MaterialAlertDialogBuilder(this)
            .setTitle(animal.identificacion)
            .setMessage("¿Qué deseas hacer?")
            .setPositiveButton("Restaurar") { _, _ ->
                restaurarAnimal(animal)
            }
            .setNegativeButton("Eliminar permanente") { _, _ ->
                eliminarPermanentemente(animal)
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun restaurarAnimal(animal: Animal) {
        lifecycleScope.launch {
            repository.restaurarAnimal(animal)
            mostrarMensaje("${animal.identificacion} restaurado", true)
        }
    }

    private fun eliminarPermanentemente(animal: Animal) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Permanentemente")
            .setMessage("Esto eliminará ${animal.identificacion} de forma permanente. ¿Continuar?")
            .setPositiveButton("Sí, eliminar") { _, _ ->
                lifecycleScope.launch {
                    repository.eliminarPermanentemente(animal)
                    mostrarMensaje("${animal.identificacion} eliminado permanentemente", true)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun vaciarPapelera() {
        lifecycleScope.launch {
            val cantidad = repository.getAnimalesEliminados().size

            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("Vaciar Papelera")
                .setMessage("Esto eliminará permanentemente $cantidad animales. ¿Continuar?")
                .setPositiveButton("Sí, vaciar") { _, _ ->
                    lifecycleScope.launch {
                        repository.vaciarPapelera()
                        mostrarMensaje("Papelera vaciada", true)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun configurarFiltros() {
        val sexos = listOf("Todos") + Animal.SEXOS
        val razas = listOf("Todas") + Animal.RAZAS

        spinnerFiltroSexo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sexos)
        spinnerFiltroRaza.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, razas)

        spinnerFiltroSexo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                actualizarLista()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerFiltroRaza.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                actualizarLista()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private var madreSeleccionada: String = "N/A"
    private var padreSeleccionado: String = "N/A"

    private fun mostrarDialogoAnimal(animal: Animal?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_animal, null)

        val etId = dialogView.findViewById<EditText>(R.id.etIdentificacion)
        val spinnerRaza = dialogView.findViewById<Spinner>(R.id.spinnerRaza)
        val spinnerSexo = dialogView.findViewById<Spinner>(R.id.spinnerSexo)
        val spinnerCategoria = dialogView.findViewById<Spinner>(R.id.spinnerCategoria)
        val etFechaNac = dialogView.findViewById<EditText>(R.id.etFechaNacimiento)
        val btnMadre = dialogView.findViewById<Button>(R.id.btnSeleccionarMadre)
        val btnPadre = dialogView.findViewById<Button>(R.id.btnSeleccionarPadre)
        val etPotrero = dialogView.findViewById<EditText>(R.id.etPotrero)
        val etFechaIng = dialogView.findViewById<EditText>(R.id.etFechaIngreso)
        val etPrecio = dialogView.findViewById<EditText>(R.id.etPrecio)
        val etNotas = dialogView.findViewById<EditText>(R.id.etNotas)

        spinnerRaza.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Animal.RAZAS)
        spinnerSexo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Animal.SEXOS)
        spinnerCategoria.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Animal.CATEGORIAS)

        etFechaNac.setOnClickListener { mostrarDatePicker(etFechaNac) }
        etFechaIng.setOnClickListener { mostrarDatePicker(etFechaIng) }

        animal?.let {
            etId.setText(it.identificacion)
            spinnerRaza.setSelection(Animal.RAZAS.indexOf(it.raza))
            spinnerSexo.setSelection(Animal.SEXOS.indexOf(it.sexo))
            spinnerCategoria.setSelection(Animal.CATEGORIAS.indexOf(it.categoria))
            etFechaNac.setText(it.fechaNacimiento)
            etPotrero.setText(it.potreroUbicacion)
            etFechaIng.setText(it.fechaIngreso)
            etPrecio.setText(it.precioCompra)
            etNotas.setText(it.notasObservaciones)

            madreSeleccionada = it.madreId
            padreSeleccionado = it.padreId

            val madre = listaAnimales.find { a -> a.id.toString() == it.madreId }
            btnMadre.text = if (madre != null) "${madre.identificacion} (${madre.id})" else "N/A"

            val padre = listaAnimales.find { a -> a.id.toString() == it.padreId }
            btnPadre.text = if (padre != null) "${padre.identificacion} (${padre.id})" else "N/A"
        } ?: run {
            madreSeleccionada = "N/A"
            padreSeleccionado = "N/A"
            btnMadre.text = "Seleccionar Madre"
            btnPadre.text = "Seleccionar Padre"
        }

        btnMadre.setOnClickListener {
            mostrarDialogoSeleccionAnimal("Hembra") { animalSeleccionado ->
                if (animalSeleccionado != null) {
                    madreSeleccionada = animalSeleccionado.id.toString()
                    btnMadre.text = "${animalSeleccionado.identificacion} (${animalSeleccionado.id})"
                } else {
                    madreSeleccionada = "N/A"
                    btnMadre.text = "N/A"
                }
            }
        }

        btnPadre.setOnClickListener {
            mostrarDialogoSeleccionAnimal("Macho") { animalSeleccionado ->
                if (animalSeleccionado != null) {
                    padreSeleccionado = animalSeleccionado.id.toString()
                    btnPadre.text = "${animalSeleccionado.identificacion} (${animalSeleccionado.id})"
                } else {
                    padreSeleccionado = "N/A"
                    btnPadre.text = "N/A"
                }
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (animal == null) "Agregar Animal" else "Editar Animal")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                guardarAnimal(animal, etId, spinnerRaza, spinnerSexo, spinnerCategoria,
                    etFechaNac, etPotrero, etFechaIng, etPrecio, etNotas)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoSeleccionAnimal(sexoFiltro: String, callback: (Animal?) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_seleccionar_animal, null)
        val etBuscar = dialogView.findViewById<EditText>(R.id.etBuscarAnimal)
        val lvAnimales = dialogView.findViewById<ListView>(R.id.lvAnimales)

        val animalesFiltrados = listaAnimales.filter { it.sexo == sexoFiltro }
        val opciones = mutableListOf("N/A") + animalesFiltrados.map { "${it.identificacion} (ID: ${it.id}) - ${it.raza}" }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, opciones)
        lvAnimales.adapter = adapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Seleccionar ${if (sexoFiltro == "Hembra") "Madre" else "Padre"}")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val busqueda = s.toString().lowercase()
                val opcionesFiltradas = if (busqueda.isEmpty()) {
                    opciones
                } else {
                    opciones.filter { it.lowercase().contains(busqueda) }
                }
                adapter.clear()
                adapter.addAll(opcionesFiltradas)
                adapter.notifyDataSetChanged()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        lvAnimales.setOnItemClickListener { _, _, position, _ ->
            val seleccion = adapter.getItem(position)
            if (seleccion == "N/A") {
                callback(null)
            } else {
                val idStr = seleccion?.substringAfter("ID: ")?.substringBefore(")")?.trim()
                val animalSeleccionado = animalesFiltrados.find { it.id.toString() == idStr }
                callback(animalSeleccionado)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun guardarAnimal(
        animalExistente: Animal?,
        etId: EditText,
        spinnerRaza: Spinner,
        spinnerSexo: Spinner,
        spinnerCategoria: Spinner,
        etFechaNac: EditText,
        etPotrero: EditText,
        etFechaIng: EditText,
        etPrecio: EditText,
        etNotas: EditText
    ) {
        val identificacion = etId.text.toString().trim()
        if (identificacion.isEmpty()) {
            Toast.makeText(this, "Ingresa una identificación", Toast.LENGTH_SHORT).show()
            return
        }

        val nuevoAnimal = Animal(
            id = animalExistente?.id ?: 0,
            identificacion = identificacion,
            raza = spinnerRaza.selectedItem.toString(),
            sexo = spinnerSexo.selectedItem.toString(),
            categoria = spinnerCategoria.selectedItem.toString(),
            fechaNacimiento = etFechaNac.text.toString(),
            madreId = madreSeleccionada,
            padreId = padreSeleccionado,
            potreroUbicacion = etPotrero.text.toString(),
            fechaIngreso = etFechaIng.text.toString(),
            precioCompra = etPrecio.text.toString(),
            notasObservaciones = etNotas.text.toString(),
            vacunas = animalExistente?.vacunas ?: mutableListOf()
        )

        lifecycleScope.launch {
            if (animalExistente != null) {
                repository.updateAnimal(nuevoAnimal)
            } else {
                repository.insertAnimal(nuevoAnimal)
            }
        }
    }

    private fun mostrarDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                editText.setText(String.format("%02d/%02d/%04d", day, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun actualizarLista() {
        llListaAnimales.removeAllViews()

        val busqueda = etBuscar.text.toString().lowercase()
        val sexoFiltro = spinnerFiltroSexo.selectedItem.toString()
        val razaFiltro = spinnerFiltroRaza.selectedItem.toString()

        val animalesFiltrados = listaAnimales.filter { animal ->
            val coincideBusqueda = busqueda.isEmpty() ||
                    animal.identificacion.lowercase().contains(busqueda) ||
                    animal.id.toString().contains(busqueda)

            val coincideSexo = sexoFiltro == "Todos" || animal.sexo == sexoFiltro
            val coincideRaza = razaFiltro == "Todas" || animal.raza == razaFiltro

            coincideBusqueda && coincideSexo && coincideRaza
        }.sortedByDescending { it.id }

        tvTotal.text = "Total de animales: ${animalesFiltrados.size}"

        if (animalesFiltrados.isEmpty()) {
            val tvVacio = TextView(this)
            tvVacio.text = "No hay animales registrados"
            tvVacio.textSize = 16f
            tvVacio.gravity = Gravity.CENTER
            tvVacio.setPadding(16, 32, 16, 32)
            llListaAnimales.addView(tvVacio)
            return
        }

        for (animal in animalesFiltrados) {
            val card = crearTarjetaAnimal(animal)
            llListaAnimales.addView(card)
        }
    }

    private fun crearTarjetaAnimal(animal: Animal): MaterialCardView {
        val card = MaterialCardView(this)
        card.radius = 24f
        card.cardElevation = 4f
        card.setCardBackgroundColor(Color.WHITE)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 24)
        card.layoutParams = params

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.HORIZONTAL
        mainLayout.setPadding(20, 20, 20, 20)

        val colorBar = android.view.View(this)
        val colorBarParams = LinearLayout.LayoutParams(12, LinearLayout.LayoutParams.MATCH_PARENT)
        colorBarParams.setMargins(0, 0, 16, 0)
        colorBar.layoutParams = colorBarParams
        colorBar.setBackgroundColor(
            if (animal.sexo == "Macho")
                resources.getColor(R.color.male, null)
            else
                resources.getColor(R.color.female, null)
        )

        val contentLayout = LinearLayout(this)
        contentLayout.orientation = LinearLayout.VERTICAL
        contentLayout.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val tvNombre = TextView(this)
        tvNombre.text = animal.identificacion
        tvNombre.textSize = 18f
        tvNombre.setTypeface(null, android.graphics.Typeface.BOLD)
        tvNombre.setTextColor(resources.getColor(R.color.text_primary, null))

        val tvInfo = TextView(this)
        tvInfo.text = "${animal.raza} • ${animal.categoria}"
        tvInfo.textSize = 14f
        tvInfo.setTextColor(resources.getColor(R.color.text_secondary, null))
        tvInfo.setPadding(0, 4, 0, 0)

        val tvUbicacion = TextView(this)
        tvUbicacion.text = "Ubicación: ${animal.potreroUbicacion}"
        tvUbicacion.textSize = 13f
        tvUbicacion.setTextColor(resources.getColor(R.color.text_secondary, null))
        tvUbicacion.setPadding(0, 8, 0, 0)

        // Indicador de sincronización
        if (!animal.sincronizado) {
            val tvSync = TextView(this)
            tvSync.text = "Pendiente de sincronizar"
            tvSync.textSize = 11f
            tvSync.setTextColor(resources.getColor(R.color.warning, null))
            tvSync.setPadding(0, 4, 0, 0)
            contentLayout.addView(tvSync)
        }

        contentLayout.addView(tvNombre)
        contentLayout.addView(tvInfo)
        contentLayout.addView(tvUbicacion)

        val iconoSexo = TextView(this)
        iconoSexo.text = if (animal.sexo == "Macho") "♂" else "♀"
        iconoSexo.textSize = 32f
        iconoSexo.setTextColor(
            if (animal.sexo == "Macho")
                resources.getColor(R.color.male, null)
            else
                resources.getColor(R.color.female, null)
        )
        iconoSexo.gravity = Gravity.CENTER

        mainLayout.addView(colorBar)
        mainLayout.addView(contentLayout)
        mainLayout.addView(iconoSexo)

        card.addView(mainLayout)

        card.setOnClickListener {
            val intent = Intent(this, DetalleAnimalActivity::class.java)
            intent.putExtra("ANIMAL_ID", animal.id)
            startActivity(intent)
        }

        return card
    }
}