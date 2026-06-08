package com.handy.medialert.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val genericName: String,
    val brandName: String? = null,
    val specification: String? = null,
    val packageUnit: String,
    val dosageForm: String,
    val packageSize: Int,
    val currentStock: Double,
    val frequencyType: FrequencyType,
    val frequencyValue: Int,
    val dailyDosage: Double,
    val startDate: LocalDate? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun dailyConsumption(): Double = when (frequencyType) {
        FrequencyType.EVERY_X_DAYS -> dailyDosage / frequencyValue
        FrequencyType.EVERY_XTH_DAY -> dailyDosage / (frequencyValue + 1)
    }

    fun depletionDate(): LocalDate {
        val effectiveStart = startDate ?: LocalDate.now()
        val daysLeft = (currentStock / dailyConsumption()).toInt()
        return effectiveStart.plusDays(daysLeft.toLong())
    }

    fun daysUntilDepletion(): Int {
        return java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.now(),
            depletionDate()
        ).toInt()
    }

    fun alert4DayDateTime(): java.time.LocalDateTime {
        return depletionDate().minusDays(4).atTime(22, 0)
    }

    fun alert1DayDateTime(): java.time.LocalDateTime {
        return depletionDate().minusDays(1).atTime(14, 0)
    }

    fun getStockDisplay(): String {
        val packages = (currentStock / packageSize).toInt()
        val remainder = (currentStock % packageSize).toInt()
        return when {
            packages > 0 && remainder > 0 -> "${packages}${packageUnit}${remainder}${dosageForm}"
            packages > 0 -> "${packages}${packageUnit}"
            else -> "${remainder}${dosageForm}"
        }
    }
}

enum class FrequencyType {
    EVERY_X_DAYS,
    EVERY_XTH_DAY
}
