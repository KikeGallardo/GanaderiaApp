package com.ganaderia.ganaderiaapp.data.model

data class ApiResponse<T>(
    val success: Boolean,
    val data: T,
    val message: String?
)