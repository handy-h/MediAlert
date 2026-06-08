package com.handy.medialert.data.dao

import androidx.room.*
import com.handy.medialert.data.entity.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActive(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE isActive = 0 ORDER BY createdAt DESC")
    fun getAllInactive(): Flow<List<Medication>>

    @Query("SELECT * FROM medications ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): Medication?

    @Insert
    suspend fun insert(medication: Medication): Long

    @Update
    suspend fun update(medication: Medication)

    @Delete
    suspend fun delete(medication: Medication)

    @Query("UPDATE medications SET currentStock = :stock WHERE id = :id")
    suspend fun updateStock(id: Long, stock: Double)

    @Query("UPDATE medications SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)
}
