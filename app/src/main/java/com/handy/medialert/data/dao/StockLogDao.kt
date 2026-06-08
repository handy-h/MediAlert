package com.handy.medialert.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.handy.medialert.data.entity.StockLog
import kotlinx.coroutines.flow.Flow

@Dao
interface StockLogDao {
    @Query("SELECT * FROM stock_logs WHERE medicationId = :medicationId ORDER BY timestamp DESC")
    fun getLogsForMedication(medicationId: Long): Flow<List<StockLog>>

    @Insert
    suspend fun insert(log: StockLog)

    @Query("DELETE FROM stock_logs WHERE medicationId = :medicationId")
    suspend fun deleteLogsForMedication(medicationId: Long)
}
