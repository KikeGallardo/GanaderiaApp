package com.ganaderia.inventarioapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SyncManager(private val context: Context) {

    companion object {
        private const val WEB_APP_URL = "https://script.google.com/macros/s/AKfycbxm4s_xKKVqbwy345jlqxoqQkkpBRuIWos91Bc-kHUdMf2NOcndJS2ki0jdCYVLD2vMRw/exec"
        private const val TAG = "SyncManager"
    }

    fun hayInternet(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun sincronizarAnimales(animales: List<Animal>): Boolean {
        if (!hayInternet()) {
            Log.d(TAG, "No hay conexión a internet")
            return false
        }

        if (WEB_APP_URL.contains("TU_URL")) {
            Log.d(TAG, "URL de sincronización no configurada")
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                // Primero descargamos los datos existentes en la nube
                val animalesNube = descargarAnimalesInterno() ?: emptyList()

                // Fusionamos: creamos un mapa por ID
                val mapaFusion = mutableMapOf<Long, Animal>()

                // Primero agregamos los de la nube
                animalesNube.forEach { mapaFusion[it.id] = it }

                // Luego sobrescribimos/agregamos los locales
                animales.forEach { mapaFusion[it.id] = it }

                // Convertimos el mapa fusionado a lista
                val animalesFusionados = mapaFusion.values.toList()

                // Ahora subimos la lista fusionada
                val jsonArray = JSONArray()
                animalesFusionados.forEach { animal ->
                    val jsonObj = JSONObject().apply {
                        put("id", animal.id)
                        put("identificacion", animal.identificacion)
                        put("raza", animal.raza)
                        put("sexo", animal.sexo)
                        put("categoria", animal.categoria)
                        put("fechaNacimiento", animal.fechaNacimiento)
                        put("madreId", animal.madreId)
                        put("padreId", animal.padreId)
                        put("potreroUbicacion", animal.potreroUbicacion)
                        put("fechaIngreso", animal.fechaIngreso)
                        put("precioCompra", animal.precioCompra)
                        put("notasObservaciones", animal.notasObservaciones)
                        put("eliminado", animal.eliminado)

                        val vacunasArray = JSONArray()
                        animal.vacunas.forEach { vacuna ->
                            vacunasArray.put(JSONObject().apply {
                                put("nombre", vacuna.nombre)
                                put("fecha", vacuna.fecha)
                                put("proximaDosis", vacuna.proximaDosis)
                            })
                        }
                        put("vacunas", vacunasArray)
                    }
                    jsonArray.put(jsonObj)
                }

                val url = URL(WEB_APP_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                connection.outputStream.use { os ->
                    val input = jsonArray.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "Sincronización completada. Código: $responseCode, Total: ${animalesFusionados.size}")

                responseCode == HttpURLConnection.HTTP_OK
            } catch (e: Exception) {
                Log.e(TAG, "Error en sincronización: ${e.message}")
                false
            }
        }
    }

    private suspend fun descargarAnimalesInterno(): List<Animal>? {
        return try {
            val url = URL("$WEB_APP_URL?action=get")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(response)

            val animales = mutableListOf<Animal>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val vacunas = mutableListOf<Vacuna>()
                if (obj.has("vacunas")) {
                    val vacunasArray = obj.getJSONArray("vacunas")
                    for (j in 0 until vacunasArray.length()) {
                        val vacObj = vacunasArray.getJSONObject(j)
                        vacunas.add(Vacuna(
                            nombre = vacObj.getString("nombre"),
                            fecha = vacObj.getString("fecha"),
                            proximaDosis = vacObj.getString("proximaDosis")
                        ))
                    }
                }

                animales.add(Animal(
                    id = obj.getLong("id"),
                    identificacion = obj.getString("identificacion"),
                    raza = obj.getString("raza"),
                    sexo = obj.getString("sexo"),
                    categoria = obj.getString("categoria"),
                    fechaNacimiento = obj.getString("fechaNacimiento"),
                    madreId = obj.getString("madreId"),
                    padreId = obj.getString("padreId"),
                    potreroUbicacion = obj.getString("potreroUbicacion"),
                    fechaIngreso = obj.getString("fechaIngreso"),
                    precioCompra = obj.getString("precioCompra"),
                    notasObservaciones = obj.getString("notasObservaciones"),
                    vacunas = vacunas,
                    eliminado = if (obj.has("eliminado")) obj.getBoolean("eliminado") else false
                ))
            }
            animales
        } catch (e: Exception) {
            Log.e(TAG, "Error en descarga interna: ${e.message}")
            null
        }
    }

    suspend fun descargarAnimales(): List<Animal>? {
        if (!hayInternet()) {
            Log.d(TAG, "No hay conexión a internet")
            return null
        }

        if (WEB_APP_URL.contains("TU_URL")) {
            Log.d(TAG, "URL de sincronización no configurada")
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$WEB_APP_URL?action=get")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)

                val animales = mutableListOf<Animal>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)

                    val vacunas = mutableListOf<Vacuna>()
                    if (obj.has("vacunas")) {
                        val vacunasArray = obj.getJSONArray("vacunas")
                        for (j in 0 until vacunasArray.length()) {
                            val vacObj = vacunasArray.getJSONObject(j)
                            vacunas.add(Vacuna(
                                nombre = vacObj.getString("nombre"),
                                fecha = vacObj.getString("fecha"),
                                proximaDosis = vacObj.getString("proximaDosis")
                            ))
                        }
                    }

                    animales.add(Animal(
                        id = obj.getLong("id"),
                        identificacion = obj.getString("identificacion"),
                        raza = obj.getString("raza"),
                        sexo = obj.getString("sexo"),
                        categoria = obj.getString("categoria"),
                        fechaNacimiento = obj.getString("fechaNacimiento"),
                        madreId = obj.getString("madreId"),
                        padreId = obj.getString("padreId"),
                        potreroUbicacion = obj.getString("potreroUbicacion"),
                        fechaIngreso = obj.getString("fechaIngreso"),
                        precioCompra = obj.getString("precioCompra"),
                        notasObservaciones = obj.getString("notasObservaciones"),
                        vacunas = vacunas
                    ))
                }

                Log.d(TAG, "Descarga completada: ${animales.size} animales")
                animales
            } catch (e: Exception) {
                Log.e(TAG, "Error descargando datos: ${e.message}")
                null
            }
        }
    }
}