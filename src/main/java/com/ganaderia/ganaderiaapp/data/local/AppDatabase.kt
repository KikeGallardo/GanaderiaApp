package com.ganaderia.ganaderiaapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ganaderia.ganaderiaapp.data.local.dao.AnimalDao
import com.ganaderia.ganaderiaapp.data.local.dao.VacunaDao
import com.ganaderia.ganaderiaapp.data.local.entities.AnimalEntity
import com.ganaderia.ganaderiaapp.data.local.entities.VacunaEntity
import com.ganaderia.ganaderiaapp.data.local.dao.KpiDao
import com.ganaderia.ganaderiaapp.data.local.entities.KPIsEntity


@Database(
    // 1. AGREGA KPIsEntity a la lista
    entities = [AnimalEntity::class, VacunaEntity::class, KPIsEntity::class],
    // 2. SUBE LA VERSIÓN a 2
    version = 2
)
abstract class GanadoDatabase : RoomDatabase() {

    abstract fun animalDao(): AnimalDao
    abstract fun vacunaDao(): VacunaDao
    abstract fun kpiDao(): KpiDao

    companion object {
        @Volatile
        private var INSTANCE: GanadoDatabase? = null

        fun getDatabase(context: Context): GanadoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GanadoDatabase::class.java,
                    "ganaderia_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}