package com.ganaderia.inventarioapp

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {
    @Query("SELECT * FROM animales WHERE eliminado = 0 ORDER BY id DESC")
    fun getAllAnimales(): Flow<List<Animal>>

    @Query("SELECT * FROM animales ORDER BY id DESC")
    fun getAllAnimalesIncludingDeleted(): Flow<List<Animal>>

    @Query("SELECT * FROM animales WHERE id = :id")
    suspend fun getAnimalById(id: Long): Animal?

    @Query("SELECT * FROM animales WHERE eliminado = 1")
    suspend fun getAnimalesEliminados(): List<Animal>

    @Query("SELECT * FROM animales WHERE sincronizado = 0")
    suspend fun getAnimalesNoSincronizados(): List<Animal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimal(animal: Animal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimales(animales: List<Animal>)

    @Update
    suspend fun updateAnimal(animal: Animal)

    @Delete
    suspend fun deleteAnimal(animal: Animal)

    @Query("DELETE FROM animales WHERE eliminado = 1")
    suspend fun deleteAllEliminados()

    @Query("DELETE FROM animales")
    suspend fun deleteAll()

    @Query("UPDATE animales SET sincronizado = 1 WHERE id = :id")
    suspend fun marcarComoSincronizado(id: Long)

    @Query("UPDATE animales SET sincronizado = 1")
    suspend fun marcarTodosComoSincronizados()
}