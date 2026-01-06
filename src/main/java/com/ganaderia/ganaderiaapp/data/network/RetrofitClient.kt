package com.ganaderia.ganaderiaapp.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // ⚠️ IMPORTANTE: Cambia esta IP por la de tu computadora
    // Para encontrar tu IP:
    // - Windows: Abre CMD y escribe "ipconfig" busca "IPv4"
    // - Mac/Linux: Abre Terminal y escribe "ifconfig" busca "inet"
    // - O usa "10.0.2.2" si estás usando el emulador de Android
    private const val BASE_URL = "http://10.0.2.2:3000/"

    // Logging para ver las peticiones en Logcat
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Cliente HTTP con timeouts
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Instancia de Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // API Service
    val api: GanadoApiService by lazy {
        retrofit.create(GanadoApiService::class.java)
    }
}

/*
INSTRUCCIONES PARA CONFIGURAR LA IP:

1. USANDO EMULADOR DE ANDROID STUDIO:
   - Usa: "http://10.0.2.2:3000/"

2. USANDO DISPOSITIVO FÍSICO:
   - Encuentra tu IP local:
     * Windows: CMD > ipconfig
     * Mac: Terminal > ifconfig
     * Linux: Terminal > ip addr
   - Asegúrate de que tu teléfono y computadora estén en la MISMA red WiFi
   - Usa: "http://TU_IP:3000/" (ejemplo: "http://192.168.1.100:3000/")

3. VERIFICA QUE LA API ESTÉ CORRIENDO:
   - Abre un navegador
   - Ve a: http://TU_IP:3000/api/health
   - Debe responder: {"success":true,"message":"API funcionando correctamente"}
*/