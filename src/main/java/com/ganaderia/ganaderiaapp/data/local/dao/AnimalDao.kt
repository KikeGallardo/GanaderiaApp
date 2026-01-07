package com.ganaderia.ganaderiaapp.data.local.dao

import androidx.room.*
import com.ganaderia.ganaderiaapp.data.local.entities.AnimalEntity

@Dao
interface AnimalDao {
    // Obtiene solo animales activos (útil para la lista principal)
    @Query("SELECT * FROM animales WHERE activo = 1 ORDER BY localId DESC")
    suspend fun getAll(): List<AnimalEntity>

    // Obtiene TODOS los animales (sincronizados y no sincronizados) para la fuente de verdad
    @Query("SELECT * FROM animales ORDER BY localId DESC")
    suspend fun getAllAnimales(): List<AnimalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(animales: List<AnimalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarAnimal(animal: AnimalEntity): Long

    // Utilizada por el SyncWorker para buscar qué subir al servidor
    @Query("SELECT * FROM animales WHERE sincronizado = 0")
    suspend fun getNoSincronizados(): List<AnimalEntity>

    // Búsqueda por ID del servidor (puede ser nulo si es offline)
    @Query("SELECT * FROM animales WHERE id = :id")
    suspend fun getAnimalById(id: Int): AnimalEntity?

    @Query("SELECT * FROM animales WHERE id = :serverId LIMIT 1")
    suspend fun getAnimalByServerId(serverId: Int): AnimalEntity?

    // Búsqueda por ID interno de la App (Siempre existe)
    @Query("SELECT * FROM animales WHERE localId = :localId")
    suspend fun getAnimalByLocalId(localId: Int): AnimalEntity?

    // LA FUNCIÓN CLAVE: Evita duplicados comparando el número de arete/identificación
    @Query("SELECT * FROM animales WHERE identificacion = :identificacion LIMIT 1")
    suspend fun getAnimalByIdentificacion(identificacion: String): AnimalEntity?

    @Query("SELECT * FROM animales WHERE sincronizado = :estado")
    suspend fun getAnimalesPorEstadoSincronizacion(estado: Boolean): List<AnimalEntity>

    @Query("UPDATE animales SET sincronizado = 1 WHERE id = :id")
    suspend fun marcarSincronizado(id: Int)

    @Query("DELETE FROM animales WHERE id = :id")
    suspend fun eliminarAnimalPorId(id: Int)

    @Query("DELETE FROM animales WHERE localId = :localId")
    suspend fun eliminarPorLocalId(localId: Int)
}