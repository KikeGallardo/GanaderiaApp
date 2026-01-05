package com.ganaderia.inventarioapp

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class VacunaConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromVacunaList(value: MutableList<Vacuna>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toVacunaList(value: String): MutableList<Vacuna> {
        val listType = object : TypeToken<MutableList<Vacuna>>() {}.type
        return gson.fromJson(value, listType)
    }
}