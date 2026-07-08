package com.handy.medialert.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_logs",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicationId")]  // 添加索引避免全表扫描
)
data class StockLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val type: StockLogType,
    val quantity: Double,
    val reason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class StockLogType {
    PURCHASE,
    MISSED_DOSE,
    LOST,
    ADJUSTMENT
}
