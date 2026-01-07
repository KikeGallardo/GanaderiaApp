package com.ganaderia.ganaderiaapp.data.local.dao

import androidx.room.*
import com.ganaderia.ganaderiaapp.data.local.entities.AnimalEntity

@Dao
interface AnimalDao {
    @Query("SELECT * FROM animales WHERE activo = 1 ORDER BY localId DESC")
    suspend fun getAll(): List<AnimalEntity>

    @Query("SELECT * FROM animales WHERE activo = 1 ORDER BY localId DESC")
    suspend fun getAllAnimales(): List<AnimalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(animales: List<AnimalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarAnimal(animal: AnimalEntity): Long

    @Query("SELECT * FROM animales WHERE (sincronizado = 0 OR sincronizado = 'false') AND activo = 1")
    suspend fun getNoSincronizados(): List<AnimalEntity>

    @Query("UPDATE animales SET madre_identificacion = :madre, padre_identificacion = :padre WHERE localId = :localId")
    suspend fun actualizarNombresPadres(localId: Int, madre: String?, padre: String?)

    @Query("SELECT * FROM animales WHERE id = :id")
    suspend fun getAnimalById(id: Int): AnimalEntity?

    @Query("SELECT * FROM animales WHERE id = :serverId LIMIT 1")
    suspend fun getAnimalByServerId(serverId: Int): AnimalEntity?

    @Query("SELECT * FROM animales WHERE localId = :localId")
    suspend fun getAnimalByLocalId(localId: Int): AnimalEntity?

    @Query("SELECT * FROM animales WHERE identificacion = :identificacion AND activo = 1 LIMIT 1")
    suspend fun getAnimalByIdentificacion(identificacion: String): AnimalEntity?

    // 🔧 CORRECCIÓN: estado ahora es Boolean pero sincronizado en BD es Int
    @Query("""
        SELECT * FROM animales 
        WHERE sincronizado = CASE WHEN :estado THEN 1 ELSE 0 END
        AND activo = 1
    """)
    suspend fun getAnimalesPorEstadoSincronizacion(estado: Boolean): List<AnimalEntity>

    @Query("UPDATE animales SET sincronizado = 1 WHERE id = :id")
    suspend fun marcarSincronizado(id: Int)

    @Query("UPDATE animales SET activo = 0 WHERE id = :id")
    suspend fun eliminarAnimalPorId(id: Int)

    @Query("UPDATE animales SET activo = 0 WHERE localId = :localId")
    suspend fun eliminarPorLocalId(localId: Int)

    @Query("SELECT COUNT(*) FROM animales WHERE activo = 1")
    suspend fun getCountAnimalesActivos(): Int

    // 🔧 Query de debug
    @Query("SELECT localId, id, identificacion, sincronizado, activo FROM animales")
    suspend fun getAllAnimalesDebug(): List<AnimalDebugInfo>
}

// Data class para debug
data class AnimalDebugInfo(
    val localId: Int,
    val id: Int?,
    val identificacion: String,
    val sincronizado: Int,  // 🔧 CAMBIADO a Int
    val activo: Int
)