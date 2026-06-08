package com.handy.medialert.data.entity

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MedicationTest {

    // ============================================================
    // dailyConsumption() 日消耗量计算测试
    // ============================================================

    @Test
    fun dailyConsumption_everyXDays_returnsCorrectRate() {
        // 每2天服用1.0片 → 日消耗 = 1.0 / 2 = 0.5
        val medication = createMedication(
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 2,
            dailyDosage = 1.0
        )
        assertEquals(0.5, medication.dailyConsumption(), 0.001)
    }

    @Test
    fun dailyConsumption_everyXDays_dailyDose() {
        // 每1天服用2.0片 → 日消耗 = 2.0 / 1 = 2.0
        val medication = createMedication(
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 2.0
        )
        assertEquals(2.0, medication.dailyConsumption(), 0.001)
    }

    @Test
    fun dailyConsumption_everyXthDay_returnsCorrectRate() {
        // 每隔2天服用3.0片 → 日消耗 = 3.0 / (2+1) = 1.0
        val medication = createMedication(
            frequencyType = FrequencyType.EVERY_XTH_DAY,
            frequencyValue = 2,
            dailyDosage = 3.0
        )
        assertEquals(1.0, medication.dailyConsumption(), 0.001)
    }

    @Test
    fun dailyConsumption_everyXthDay_intervalOne() {
        // 每隔1天服用1.0片 → 日消耗 = 1.0 / (1+1) = 0.5
        val medication = createMedication(
            frequencyType = FrequencyType.EVERY_XTH_DAY,
            frequencyValue = 1,
            dailyDosage = 1.0
        )
        assertEquals(0.5, medication.dailyConsumption(), 0.001)
    }

    @Test
    fun dailyConsumption_zeroDivisor_fallbackToDailyDose() {
        // frequencyValue=0 时 divisor=0，应降级返回 dailyDosage
        val medication = createMedication(
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 0,
            dailyDosage = 2.0
        )
        assertEquals(2.0, medication.dailyConsumption(), 0.001)
    }

    // ============================================================
    // depletionDate() 耗尽日期计算测试
    // ============================================================

    @Test
    fun depletionDate_withStartDate_calculatesCorrectly() {
        // startDate=2026-06-01, stock=28, dailyConsumption=1.0 → 耗尽日=2026-06-29
        val medication = createMedication(
            currentStock = 28.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = LocalDate.of(2026, 6, 1)
        )
        assertEquals(LocalDate.of(2026, 6, 29), medication.depletionDate())
    }

    @Test
    fun depletionDate_withoutStartDate_usesToday() {
        // startDate=null 时使用 LocalDate.now()
        val medication = createMedication(
            currentStock = 10.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = null
        )
        val expectedDaysLeft = 10 // 10 / 1.0 = 10 天
        val expected = LocalDate.now().plusDays(expectedDaysLeft.toLong())
        assertEquals(expected, medication.depletionDate())
    }

    @Test
    fun depletionDate_zeroStock_returnsToday() {
        // 库存为0时，耗尽日 = 起始日（今天）
        val medication = createMedication(
            currentStock = 0.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = null
        )
        assertEquals(LocalDate.now(), medication.depletionDate())
    }

    @Test
    fun depletionDate_fractionalDays_truncates() {
        // stock=5, dailyConsumption=2.0 → 5/2=2.5 → 取整=2天
        val startDate = LocalDate.of(2026, 6, 1)
        val medication = createMedication(
            currentStock = 5.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 2.0,
            startDate = startDate
        )
        assertEquals(LocalDate.of(2026, 6, 3), medication.depletionDate())
    }

    // ============================================================
    // daysUntilDepletion() 距耗尽天数测试
    // ============================================================

    @Test
    fun daysUntilDepletion_futureDate_returnsPositive() {
        // 构造一个耗尽日期在 10 天后的药品
        val today = LocalDate.now()
        val medication = createMedication(
            currentStock = 10.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = today
        )
        assertEquals(10, medication.daysUntilDepletion())
    }

    @Test
    fun daysUntilDepletion_pastDate_returnsNegative() {
        // startDate 在很久之前，库存很少
        val pastDate = LocalDate.now().minusDays(100)
        val medication = createMedication(
            currentStock = 1.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = pastDate
        )
        assertTrue(medication.daysUntilDepletion() < 0)
    }

    // ============================================================
    // getStockDisplay() 库存显示格式化测试
    // ============================================================

    @Test
    fun getStockDisplay_packagesAndRemainder_showsMixed() {
        // 34片 ÷ 14片/盒 = 2盒余6片 → "2盒6片"
        val medication = createMedication(
            currentStock = 34.0,
            packageUnit = "盒",
            dosageForm = "片",
            packageSize = 14
        )
        assertEquals("2盒6片", medication.getStockDisplay())
    }

    @Test
    fun getStockDisplay_packagesOnly_showsPackages() {
        // 28片 ÷ 14片/盒 = 2盒余0片 → "2盒"
        val medication = createMedication(
            currentStock = 28.0,
            packageUnit = "盒",
            dosageForm = "片",
            packageSize = 14
        )
        assertEquals("2盒", medication.getStockDisplay())
    }

    @Test
    fun getStockDisplay_remainderOnly_showsRemainder() {
        // 6片 ÷ 14片/盒 = 0盒余6片 → "6片"
        val medication = createMedication(
            currentStock = 6.0,
            packageUnit = "盒",
            dosageForm = "片",
            packageSize = 14
        )
        assertEquals("6片", medication.getStockDisplay())
    }

    @Test
    fun getStockDisplay_zeroStock_showsZeroRemainder() {
        // 0片 → "0片"
        val medication = createMedication(
            currentStock = 0.0,
            packageUnit = "盒",
            dosageForm = "片",
            packageSize = 14
        )
        assertEquals("0片", medication.getStockDisplay())
    }

    @Test
    fun getStockDisplay_singlePackage_showsCorrectly() {
        // 14片 ÷ 14片/盒 = 1盒 → "1盒"
        val medication = createMedication(
            currentStock = 14.0,
            packageUnit = "盒",
            dosageForm = "片",
            packageSize = 14
        )
        assertEquals("1盒", medication.getStockDisplay())
    }

    @Test
    fun getStockDisplay_mlUnit_showsCorrectly() {
        // 15ml ÷ 10ml/瓶 = 1瓶余5ml → "1瓶5ml"
        val medication = createMedication(
            currentStock = 15.0,
            packageUnit = "瓶",
            dosageForm = "ml",
            packageSize = 10
        )
        assertEquals("1瓶5ml", medication.getStockDisplay())
    }

    // ============================================================
    // alert4DayDateTime() 第一阶段提醒时间测试
    // ============================================================

    @Test
    fun alert4DayDateTime_returnsCorrectTime() {
        // 耗尽日期=2026-06-29 → 提前4天=2026-06-25 22:00
        val medication = createMedication(
            currentStock = 28.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = LocalDate.of(2026, 6, 1)
        )
        val alertTime = medication.alert4DayDateTime()
        assertEquals(LocalDate.of(2026, 6, 25), alertTime.toLocalDate())
        assertEquals(22, alertTime.hour)
        assertEquals(0, alertTime.minute)
    }

    // ============================================================
    // alert1DayDateTime() 第二阶段提醒时间测试
    // ============================================================

    @Test
    fun alert1DayDateTime_returnsCorrectTime() {
        // 耗尽日期=2026-06-29 → 提前1天=2026-06-28 14:00
        val medication = createMedication(
            currentStock = 28.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = LocalDate.of(2026, 6, 1)
        )
        val alertTime = medication.alert1DayDateTime()
        assertEquals(LocalDate.of(2026, 6, 28), alertTime.toLocalDate())
        assertEquals(14, alertTime.hour)
        assertEquals(0, alertTime.minute)
    }

    // ============================================================
    // 预警状态颜色判定辅助测试
    // ============================================================

    @Test
    fun alertColorStatus_urgent_whenDaysLessOrEqualOne() {
        // daysUntilDepletion ≤ 1 → 紧急(红色)
        val medication = createMedication(
            currentStock = 1.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = LocalDate.now()
        )
        val days = medication.daysUntilDepletion()
        assertTrue("days=$days 应为紧急状态", days <= 1)
    }

    @Test
    fun alertColorStatus_warning_whenDaysBetweenTwoAndFour() {
        // 2 ≤ daysUntilDepletion ≤ 4 → 警告(黄色)
        val medication = createMedication(
            currentStock = 3.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = LocalDate.now()
        )
        val days = medication.daysUntilDepletion()
        assertTrue("days=$days 应为警告状态", days in 2..4)
    }

    @Test
    fun alertColorStatus_normal_whenDaysGreaterThanFour() {
        // daysUntilDepletion > 4 → 正常(绿色)
        val medication = createMedication(
            currentStock = 10.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = LocalDate.now()
        )
        val days = medication.daysUntilDepletion()
        assertTrue("days=$days 应为正常状态", days > 4)
    }

    // ============================================================
    // 辅助工厂方法
    // ============================================================

    private fun createMedication(
        id: Long = 1L,
        genericName: String = "测试药品",
        brandName: String? = null,
        specification: String? = null,
        packageUnit: String = "盒",
        dosageForm: String = "片",
        packageSize: Int = 14,
        currentStock: Double = 28.0,
        frequencyType: FrequencyType = FrequencyType.EVERY_X_DAYS,
        frequencyValue: Int = 1,
        dailyDosage: Double = 1.0,
        startDate: LocalDate? = LocalDate.of(2026, 6, 1),
        isActive: Boolean = true
    ): Medication = Medication(
        id = id,
        genericName = genericName,
        brandName = brandName,
        specification = specification,
        packageUnit = packageUnit,
        dosageForm = dosageForm,
        packageSize = packageSize,
        currentStock = currentStock,
        frequencyType = frequencyType,
        frequencyValue = frequencyValue,
        dailyDosage = dailyDosage,
        startDate = startDate,
        isActive = isActive
    )
}
