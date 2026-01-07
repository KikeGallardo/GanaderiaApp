package com.ganaderia.ganaderiaapp.data.local.dao

import androidx.room.*
import com.ganaderia.ganaderiaapp.data.local.entities.AnimalEntity

@Dao
interface AnimalDao {
    // Obtiene solo animales activos (útil para la lista principal)
    @Query("SELECT * FROM animales WHERE activo = 1 ORDER BY localId DESC")
    suspend fun getAll(): List<AnimalEntity>

    // CORRECCIÓN: getAllAnimales también debe filtrar por activo
    @Query("SELECT * FROM animales WHERE activo = 1 ORDER BY localId DESC")
    suspend fun getAllAnimales(): List<AnimalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(animales: List<AnimalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarAnimal(animal: AnimalEntity): Long

    @Query("SELECT * FROM animales WHERE sincronizado = 0 AND activo = 1")
    suspend fun getNoSincronizados(): List<AnimalEntity>

    @Query("SELECT * FROM animales WHERE id = :id")
    suspend fun getAnimalById(id: Int): AnimalEntity?

    @Query("SELECT * FROM animales WHERE id = :serverId LIMIT 1")
    suspend fun getAnimalByServerId(serverId: Int): AnimalEntity?

    @Query("SELECT * FROM animales WHERE localId = :localId")
    suspend fun getAnimalByLocalId(localId: Int): AnimalEntity?

    @Query("SELECT * FROM animales WHERE identificacion = :identificacion AND activo = 1 LIMIT 1")
    suspend fun getAnimalByIdentificacion(identificacion: String): AnimalEntity?

    @Query("SELECT * FROM animales WHERE sincronizado = :estado AND activo = 1")
    suspend fun getAnimalesPorEstadoSincronizacion(estado: Boolean): List<AnimalEntity>

    @Query("UPDATE animales SET sincronizado = 1 WHERE id = :id")
    suspend fun marcarSincronizado(id: Int)

    // CORRECCIÓN: Marcar como inactivo en lugar de borrar físicamente
    @Query("UPDATE animales SET activo = 0 WHERE id = :id")
    suspend fun eliminarAnimalPorId(id: Int)

    // CORRECCIÓN: Marcar como inactivo por localId
    @Query("UPDATE animales SET activo = 0 WHERE localId = :localId")
    suspend fun eliminarPorLocalId(localId: Int)

    // Nueva función para obtener el conteo total de animales activos
    @Query("SELECT COUNT(*) FROM animales WHERE activo = 1")
    suspend fun getCountAnimalesActivos(): Int
}