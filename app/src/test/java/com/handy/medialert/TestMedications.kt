package com.handy.medialert

import com.handy.medialert.data.entity.FrequencyType
import com.handy.medialert.data.entity.Medication
import java.time.LocalDate

/**
 * 单元测试共用的药品样例工厂。
 * 默认构造一个"活跃、未来耗尽、未设置日历事件"的药品，便于测试提醒注册逻辑。
 */
object TestMedications {

    fun sample(
        id: Long = 1L,
        genericName: String = "测试药品",
        packageUnit: String = "盒",
        dosageForm: String = "片",
        packageSize: Double = 10.0,
        currentStock: Double = 30.0,
        frequencyType: FrequencyType = FrequencyType.EVERY_X_DAYS,
        frequencyValue: Int = 1,
        dailyDosage: Double = 1.0,
        startDate: LocalDate? = LocalDate.now(),
        isActive: Boolean = true,
        calendarEventId: Long? = null
    ): Medication = Medication(
        id = id,
        genericName = genericName,
        packageUnit = packageUnit,
        dosageForm = dosageForm,
        packageSize = packageSize,
        currentStock = currentStock,
        frequencyType = frequencyType,
        frequencyValue = frequencyValue,
        dailyDosage = dailyDosage,
        startDate = startDate,
        isActive = isActive,
        calendarEventId = calendarEventId
    )
}
