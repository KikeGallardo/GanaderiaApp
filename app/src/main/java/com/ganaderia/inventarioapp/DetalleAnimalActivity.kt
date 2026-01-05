package com.ganaderia.inventarioapp

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.*
import androidx.lifecycle.lifecycleScope

class DetalleAnimalActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvHeaderIcono: TextView
    private lateinit var tvHeaderNombre: TextView
    private lateinit var tvHeaderSubtitulo: TextView
    private lateinit var llInfoBasica: LinearLayout
    private lateinit var tvArbolGenealogico: TextView
    private lateinit var llVacunas: LinearLayout
    private lateinit var btnAgregarVacuna: MaterialButton
    private lateinit var btnEditar: MaterialButton
    private lateinit var btnEliminar: MaterialButton

    private var animal: Animal? = null
    private val listaAnimales = mutableListOf<Animal>()
    private val PREFS_NAME = "InventarioGanaderoPrefs"
    private val KEY_ANIMALES = "animales"

    // Este método convierte el objeto a una cadena de texto (ej: "Firulais,Perro")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_animal)

        toolbar = findViewById(R.id.toolbar)
        tvHeaderIcono = findViewById(R.id.tvHeaderIcono)
        tvHeaderNombre = findViewById(R.id.tvHeaderNombre)
        tvHeaderSubtitulo = findViewById(R.id.tvHeaderSubtitulo)
        llInfoBasica = findViewById(R.id.llInfoBasica)
        tvArbolGenealogico = findViewById(R.id.tvArbolGenealogico)
        llVacunas = findViewById(R.id.llVacunas)
        btnAgregarVacuna = findViewById(R.id.btnAgregarVacuna)
        btnEditar = findViewById(R.id.btnEditar)
        btnEliminar = findViewById(R.id.btnEliminar)

        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val animalId = intent.getLongExtra("ANIMAL_ID", -1)
        cargarAnimales()
        animal = listaAnimales.find { it.id == animalId }

        if (animal == null) {
            Toast.makeText(this, "Animal no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mostrarDetalles()

        btnAgregarVacuna.setOnClickListener {
            mostrarDialogoVacuna()
        }

        btnEditar.setOnClickListener {
            mostrarDialogoEditarAnimal()
        }

        btnEliminar.setOnClickListener {
            eliminarAnimal()
        }
    }

    private fun mostrarDetalles() {
        animal?.let { a ->
            // Header
            tvHeaderIcono.text = if (a.sexo == "Macho") "🐂" else "🐄"
            tvHeaderNombre.text = a.identificacion
            tvHeaderSubtitulo.text = "${a.raza} • ${a.categoria}"

            // Información básica con diseño moderno
            llInfoBasica.removeAllViews()

            agregarInfoItem("🆔", "Identificación", a.identificacion)
            agregarInfoItem("🐄", "Raza", a.raza)
            agregarInfoItem("⚥", "Sexo", a.sexo)
            agregarInfoItem("📊", "Categoría", a.categoria)
            agregarInfoItem("🎂", "Fecha de Nacimiento", a.fechaNacimiento)

            val nombreMadre = if (a.madreId != "N/A") {
                listaAnimales.find { it.id.toString() == a.madreId && !it.eliminado }?.identificacion ?: "Desconocida"
            } else "N/A"
            agregarInfoItem("👩", "Madre", nombreMadre)

            val nombrePadre = if (a.padreId != "N/A") {
                listaAnimales.find { it.id.toString() == a.padreId && !it.eliminado }?.identificacion ?: "Desconocido"
            } else "N/A"
            agregarInfoItem("👨", "Padre", nombrePadre)

            agregarInfoItem("📍", "Ubicación", a.potreroUbicacion)
            agregarInfoItem("📅", "Fecha de Ingreso", a.fechaIngreso)

            if (a.precioCompra.isNotEmpty()) {
                agregarInfoItem("💰", "Precio de Compra", "$${a.precioCompra}")
            }

            if (a.notasObservaciones.isNotEmpty()) {
                agregarInfoItem("📝", "Observaciones", a.notasObservaciones, true)
            }

            mostrarArbolGenealogico(a)
            mostrarVacunas(a)
        }
    }

    private fun agregarInfoItem(icono: String, titulo: String, valor: String, esLargo: Boolean = false) {
        val itemLayout = LinearLayout(this)
        itemLayout.orientation = if (esLargo) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        itemLayout.setPadding(0, 12, 0, 12)

        val iconoText = TextView(this)
        iconoText.text = icono
        iconoText.textSize = 20f
        iconoText.gravity = Gravity.CENTER
        if (!esLargo) {
            iconoText.layoutParams = LinearLayout.LayoutParams(60, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val contentLayout = LinearLayout(this)
        contentLayout.orientation = LinearLayout.VERTICAL
        if (!esLargo) {
            contentLayout.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        } else {
            contentLayout.setPadding(0, 8, 0, 0)
        }

        val tituloText = TextView(this)
        tituloText.text = titulo
        tituloText.textSize = 12f
        tituloText.setTextColor(resources.getColor(R.color.text_secondary, null))

        val valorText = TextView(this)
        valorText.text = valor
        valorText.textSize = 16f
        valorText.setTextColor(resources.getColor(R.color.text_primary, null))
        valorText.setTypeface(null, android.graphics.Typeface.BOLD)

        contentLayout.addView(tituloText)
        contentLayout.addView(valorText)

        if (esLargo) {
            itemLayout.addView(iconoText)
            itemLayout.addView(contentLayout)
        } else {
            itemLayout.addView(iconoText)
            itemLayout.addView(contentLayout)
        }

        llInfoBasica.addView(itemLayout)
    }

    private fun mostrarArbolGenealogico(animal: Animal) {
        val sb = StringBuilder()

        val madre = if (animal.madreId != "N/A") {
            listaAnimales.find { it.id.toString() == animal.madreId && !it.eliminado }
        } else null

        val padre = if (animal.padreId != "N/A") {
            listaAnimales.find { it.id.toString() == animal.padreId && !it.eliminado }
        } else null

        sb.append("        ${animal.identificacion}\n")
        sb.append("        └─ ${animal.raza}, ${animal.sexo}\n\n")

        sb.append("Padres:\n")
        sb.append("├─ Madre: ${madre?.identificacion ?: "N/A"}\n")
        if (madre != null) {
            sb.append("│  └─ ${madre.raza}, ${madre.categoria}\n")
        }
        sb.append("│\n")
        sb.append("└─ Padre: ${padre?.identificacion ?: "N/A"}\n")
        if (padre != null) {
            sb.append("   └─ ${padre.raza}, ${padre.categoria}\n")
        }

        if (madre != null) {
            sb.append("\nAbuelos Maternos:\n")
            val abuelaMat = if (madre.madreId != "N/A") {
                listaAnimales.find { it.id.toString() == madre.madreId && !it.eliminado }
            } else null
            val abueloMat = if (madre.padreId != "N/A") {
                listaAnimales.find { it.id.toString() == madre.padreId && !it.eliminado }
            } else null

            sb.append("├─ ${abuelaMat?.identificacion ?: "N/A"} (Abuela)\n")
            sb.append("└─ ${abueloMat?.identificacion ?: "N/A"} (Abuelo)\n")
        }

        if (padre != null) {
            sb.append("\nAbuelos Paternos:\n")
            val abuelaPat = if (padre.madreId != "N/A") {
                listaAnimales.find { it.id.toString() == padre.madreId && !it.eliminado }
            } else null
            val abueloPat = if (padre.padreId != "N/A") {
                listaAnimales.find { it.id.toString() == padre.padreId && !it.eliminado }
            } else null

            sb.append("├─ ${abuelaPat?.identificacion ?: "N/A"} (Abuela)\n")
            sb.append("└─ ${abueloPat?.identificacion ?: "N/A"} (Abuelo)\n")
        }

        val hijos = listaAnimales.filter {
            (it.madreId == animal.id.toString() || it.padreId == animal.id.toString()) && !it.eliminado
        }

        if (hijos.isNotEmpty()) {
            sb.append("\n👶 Descendencia (${hijos.size}):\n")
            hijos.forEach { hijo ->
                sb.append("├─ ${hijo.identificacion} (${hijo.sexo})\n")
            }
        }

        tvArbolGenealogico.text = sb.toString()
    }

    private fun mostrarVacunas(animal: Animal) {
        llVacunas.removeAllViews()

        if (animal.vacunas.isEmpty()) {
            val tvVacio = TextView(this)
            tvVacio.text = "No hay vacunas registradas"
            tvVacio.gravity = Gravity.CENTER
            tvVacio.setTextColor(resources.getColor(R.color.text_secondary, null))
            tvVacio.setPadding(0, 16, 0, 16)
            llVacunas.addView(tvVacio)
            return
        }

        animal.vacunas.forEachIndexed { index, vacuna ->
            val card = MaterialCardView(this)
            card.radius = 12f
            card.cardElevation = 2f
            card.setCardBackgroundColor(resources.getColor(R.color.surface, null))

            val cardParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            cardParams.setMargins(0, 0, 0, 12)
            card.layoutParams = cardParams

            val cardLayout = LinearLayout(this)
            cardLayout.orientation = LinearLayout.HORIZONTAL
            cardLayout.setPadding(16, 16, 16, 16)

            val contentLayout = LinearLayout(this)
            contentLayout.orientation = LinearLayout.VERTICAL
            contentLayout.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            val tvNombre = TextView(this)
            tvNombre.text = vacuna.nombre
            tvNombre.textSize = 16f
            tvNombre.setTypeface(null, android.graphics.Typeface.BOLD)
            tvNombre.setTextColor(resources.getColor(R.color.text_primary, null))

            val tvFecha = TextView(this)
            tvFecha.text = "Aplicada: ${vacuna.fecha}"
            tvFecha.textSize = 13f
            tvFecha.setTextColor(resources.getColor(R.color.text_secondary, null))
            tvFecha.setPadding(0, 4, 0, 0)

            val tvProxima = TextView(this)
            tvProxima.text = "Próxima: ${vacuna.proximaDosis}"
            tvProxima.textSize = 13f
            tvProxima.setTextColor(resources.getColor(R.color.accent, null))
            tvProxima.setPadding(0, 4, 0, 0)

            contentLayout.addView(tvNombre)
            contentLayout.addView(tvFecha)
            contentLayout.addView(tvProxima)

            val btnEliminarVacuna = MaterialButton(this)
            btnEliminarVacuna.text = "🗑️"
            btnEliminarVacuna.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            btnEliminarVacuna.setBackgroundColor(Color.TRANSPARENT)
            btnEliminarVacuna.setTextColor(resources.getColor(R.color.error, null))
            btnEliminarVacuna.setOnClickListener {
                eliminarVacuna(index)
            }

            cardLayout.addView(contentLayout)
            cardLayout.addView(btnEliminarVacuna)
            card.addView(cardLayout)

            llVacunas.addView(card)
        }
    }

    private fun eliminarVacuna(index: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Vacuna")
            .setMessage("¿Estás seguro de eliminar esta vacuna?")
            .setPositiveButton("Eliminar") { _, _ ->
                animal?.vacunas?.removeAt(index)
                guardarDatos()
                mostrarDetalles()
                Toast.makeText(this, "Vacuna eliminada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoVacuna() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 32, 32, 32)

        val tvTipo = TextView(this)
        tvTipo.text = "Tipo de Vacuna"
        tvTipo.textSize = 16f
        tvTipo.setPadding(0, 0, 0, 8)
        layout.addView(tvTipo)

        val spinnerTipo = Spinner(this)
        spinnerTipo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Vacuna.TIPOS_VACUNAS)
        layout.addView(spinnerTipo)

        val tvObs = TextView(this)
        tvObs.text = "Observaciones (opcional)"
        tvObs.textSize = 14f
        tvObs.setPadding(0, 16, 0, 8)
        layout.addView(tvObs)

        val etObservaciones = EditText(this)
        etObservaciones.hint = "Observaciones..."
        etObservaciones.minHeight = 100
        layout.addView(etObservaciones)

        val tvFecha = TextView(this)
        tvFecha.text = "Fecha de aplicación"
        tvFecha.textSize = 14f
        tvFecha.setPadding(0, 16, 0, 8)
        layout.addView(tvFecha)

        val etFecha = EditText(this)
        etFecha.hint = "DD/MM/AAAA"
        etFecha.isFocusable = false
        etFecha.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    etFecha.setText(String.format("%02d/%02d/%04d", day, month + 1, year))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        layout.addView(etFecha)

        val tvProxima = TextView(this)
        tvProxima.text = "Próxima dosis"
        tvProxima.textSize = 14f
        tvProxima.setPadding(0, 16, 0, 8)
        layout.addView(tvProxima)

        val etProxima = EditText(this)
        etProxima.hint = "DD/MM/AAAA"
        etProxima.isFocusable = false
        etProxima.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    etProxima.setText(String.format("%02d/%02d/%04d", day, month + 1, year))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        layout.addView(etProxima)

        AlertDialog.Builder(this)
            .setTitle("Registrar Vacuna")
            .setView(layout)
            .setPositiveButton("Registrar") { _, _ ->
                val tipo = spinnerTipo.selectedItem.toString()
                val fecha = etFecha.text.toString()
                val proxima = etProxima.text.toString()
                val obs = etObservaciones.text.toString()

                val nombreCompleto = if (obs.isNotEmpty()) "$tipo - $obs" else tipo

                if (fecha.isNotEmpty()) {
                    animal?.vacunas?.add(Vacuna(nombreCompleto, fecha, proxima))
                    guardarDatos()
                    mostrarDetalles()
                    Toast.makeText(this, "Vacuna registrada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Ingresa la fecha de aplicación", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEditarAnimal() {
        animal?.let { animalActual ->
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

            // Configurar spinners
            spinnerRaza.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Animal.RAZAS)
            spinnerSexo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Animal.SEXOS)
            spinnerCategoria.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Animal.CATEGORIAS)

            // Configurar date pickers
            etFechaNac.setOnClickListener { mostrarDatePicker(etFechaNac) }
            etFechaIng.setOnClickListener { mostrarDatePicker(etFechaIng) }

            // Cargar datos actuales
            etId.setText(animalActual.identificacion)
            spinnerRaza.setSelection(Animal.RAZAS.indexOf(animalActual.raza))
            spinnerSexo.setSelection(Animal.SEXOS.indexOf(animalActual.sexo))
            spinnerCategoria.setSelection(Animal.CATEGORIAS.indexOf(animalActual.categoria))
            etFechaNac.setText(animalActual.fechaNacimiento)
            etPotrero.setText(animalActual.potreroUbicacion)
            etFechaIng.setText(animalActual.fechaIngreso)
            etPrecio.setText(animalActual.precioCompra)
            etNotas.setText(animalActual.notasObservaciones)

            var madreSeleccionada = animalActual.madreId
            var padreSeleccionado = animalActual.padreId

            val madre = listaAnimales.find { it.id.toString() == animalActual.madreId && !it.eliminado }
            btnMadre.text = if (madre != null) "${madre.identificacion} (${madre.id})" else "N/A"

            val padre = listaAnimales.find { it.id.toString() == animalActual.padreId && !it.eliminado }
            btnPadre.text = if (padre != null) "${padre.identificacion} (${padre.id})" else "N/A"

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

            AlertDialog.Builder(this)
                .setTitle("Editar Animal")
                .setView(dialogView)
                .setPositiveButton("Guardar") { _, _ ->
                    val identificacion = etId.text.toString().trim()
                    if (identificacion.isEmpty()) {
                        Toast.makeText(this, "Ingresa una identificación", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val animalEditado = Animal(
                        id = animalActual.id,
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
                        vacunas = animalActual.vacunas,
                        eliminado = animalActual.eliminado
                    )

                    val index = listaAnimales.indexOfFirst { it.id == animalActual.id }
                    if (index >= 0) {
                        listaAnimales[index] = animalEditado
                        animal = animalEditado
                        guardarDatos()
                        mostrarDetalles()
                        Toast.makeText(this, "✅ Animal actualizado", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
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

    private fun mostrarDialogoSeleccionAnimal(sexoFiltro: String, callback: (Animal?) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_seleccionar_animal, null)
        val etBuscar = dialogView.findViewById<EditText>(R.id.etBuscarAnimal)
        val lvAnimales = dialogView.findViewById<ListView>(R.id.lvAnimales)

        val animalesFiltrados = listaAnimales.filter { it.sexo == sexoFiltro && !it.eliminado && it.id != animal?.id }
        val opciones = mutableListOf("N/A") + animalesFiltrados.map { "${it.identificacion} (ID: ${it.id}) - ${it.raza}" }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, opciones)
        lvAnimales.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setTitle("Seleccionar ${if (sexoFiltro == "Hembra") "Madre" else "Padre"}")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()

        etBuscar.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
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

    private fun eliminarAnimal() {
        AlertDialog.Builder(this)
            .setTitle("Mover a Papelera")
            .setMessage("¿Mover a ${animal?.identificacion} a la papelera de reciclaje?")
            .setPositiveButton("Sí, mover") { _, _ ->
                animal?.let {
                    val index = listaAnimales.indexOfFirst { a -> a.id == it.id }
                    if (index >= 0) {
                        listaAnimales[index] = it.copy(eliminado = true)
                        guardarDatos()
                        Toast.makeText(this, "🗑️ ${it.identificacion} movido a papelera", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarDatos() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val animalesString = listaAnimales.joinToString(";") { it.toStringData() }
        editor.putString(KEY_ANIMALES, animalesString)
        editor.apply()
    }

    // Sustituye cargarAnimales() y la búsqueda manual por:
    private fun cargarDatosAnimal(animalId: Long) {
        val repository = AnimalRepository(this)
        lifecycleScope.launch {
            animal = repository.getAnimalById(animalId)
            if (animal != null) {
                mostrarDetalles()
            } else {
                Toast.makeText(this@DetalleAnimalActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}