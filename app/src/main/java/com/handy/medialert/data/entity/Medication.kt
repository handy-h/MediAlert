package com.handy.medialert.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val genericName: String,
    val brandName: String? = null,
    val specification: String? = null,
    val packageUnit: String,
    val dosageForm: String,
    val packageSize: Double,
    val currentStock: Double,
    val frequencyType: FrequencyType,
    val frequencyValue: Int,
    val dailyDosage: Double,
    val startDate: LocalDate? = null,
    val isActive: Boolean = true,
    val calendarEventId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun dailyConsumption(): Double {
        val divisor = when (frequencyType) {
            FrequencyType.EVERY_X_DAYS -> frequencyValue
            FrequencyType.EVERY_XTH_DAY -> frequencyValue + 1
        }
        return if (divisor > 0) dailyDosage / divisor else dailyDosage
    }

    fun depletionDate(): LocalDate {
        // 没有手动设置开始日期时，使用创建日期作为固定起始日，
        // 避免每天都重新取 LocalDate.now() 导致倒计时永远不动。
        val effectiveStart = startDate ?: Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val daysLeft = kotlin.math.ceil(currentStock / dailyConsumption()).toInt()
        return effectiveStart.plusDays(daysLeft.toLong())
    }

    fun daysUntilDepletion(): Int {
        return java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.now(),
            depletionDate()
        ).toInt().coerceAtLeast(0)
    }

    fun alert4DayDateTime(): java.time.LocalDateTime {
        return depletionDate().minusDays(4).atTime(22, 0)
    }

    fun alert1DayDateTime(): java.time.LocalDateTime {
        return depletionDate().minusDays(1).atTime(14, 0)
    }

    fun getStockDisplay(): String {
        // toInt() 向下取整，仅计算完整包装数；余数由 % 精确计算
        val packages = (currentStock / packageSize).toInt()
        val remainder = currentStock % packageSize
        return when {
            packages > 0 && remainder > 0.0 -> "${packages}${packageUnit}${formatRemainder(remainder)}${dosageForm}"
            packages > 0 -> "${packages}${packageUnit}"
            remainder > 0.0 -> "${formatRemainder(remainder)}${dosageForm}"
            else -> "0${dosageForm}"
        }
    }

    private fun formatRemainder(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    }
}

enum class FrequencyType {
    EVERY_X_DAYS,
    EVERY_XTH_DAY
}
