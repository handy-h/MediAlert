package com.handy.medialert.data.entity

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class MedicationTest {

    @Test
    fun dailyConsumption_everyXDays_returnsCorrectRate() {
        val medication = createMedication(
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 2,
            dailyDosage = 1.0
        )
        assertEquals(0.5, medication.dailyConsumption(), 0.001)
    }

    @Test
    fun dailyConsumption_everyXDays_dailyDose() {
        val medication = createMedication(
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 2.0
        )
        assertEquals(2.0, medication.dailyConsumption(), 0.001)
    }

    @Test
    fun dailyConsumption_everyXthDay_returnsCorrectRate() {
        val medication = createMedication(
            frequencyType = FrequencyType.EVERY_XTH_DAY,
            frequencyValue = 2,
            dailyDosage = 3.0
        )
        assertEquals(1.0, medication.dailyConsumption(), 0.001)
    }

    @Test
    fun dailyConsumption_everyXthDay_intervalOne() {
        val medication = createMedication(
            frequencyType = FrequencyType.EVERY_XTH_DAY,
            frequencyValue = 1,
            dailyDosage = 1.0
        )
        assertEquals(0.5, medication.dailyConsumption(), 0.001)
    }

    @Test
    fun dailyConsumption_zeroDivisor_fallbackToDailyDose() {
        val medication = createMedication(
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 0,
            dailyDosage = 2.0
        )
        assertEquals(2.0, medication.dailyConsumption(), 0.001)
    }

    @Test
    fun depletionDate_withStartDate_calculatesCorrectly() {
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
    fun depletionDate_withoutStartDate_usesCreatedAtDate() {
        val createdAtDate = LocalDate.of(2026, 5, 1)
        val createdAt = createdAtDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val medication = createMedication(
            currentStock = 10.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = null,
            createdAt = createdAt
        )
        assertEquals(createdAtDate.plusDays(10), medication.depletionDate())
    }

    @Test
    fun daysUntilDepletion_countdownDecreasesOverTime() {
        val fiveDaysAgo = LocalDate.now().minusDays(5)
        val createdAt = fiveDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val medication = createMedication(
            currentStock = 10.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = null,
            createdAt = createdAt
        )
        // 5 天前创建，10 片每天 1 片，depletionDate = 创建日+10，所以剩余天数应为 5
        val expected = ChronoUnit.DAYS.between(LocalDate.now(), fiveDaysAgo.plusDays(10)).toInt()
        assertEquals(expected, medication.daysUntilDepletion())
    }

    @Test
    fun depletionDate_zeroStock_returnsCreatedAtDate() {
        val createdAtDate = LocalDate.of(2026, 6, 1)
        val createdAt = createdAtDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val medication = createMedication(
            currentStock = 0.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = null,
            createdAt = createdAt
        )
        // 无开始日期时耗尽日应等于创建日（0 天消耗）
        assertEquals(createdAtDate, medication.depletionDate())
    }

    @Test
    fun depletionDate_fractionalDays_roundsUp() {
        val startDate = LocalDate.of(2026, 6, 1)
        val medication = createMedication(
            currentStock = 5.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 2.0,
            startDate = startDate
        )
        assertEquals(LocalDate.of(2026, 6, 4), medication.depletionDate())
    }

    @Test
    fun daysUntilDepletion_futureDate_returnsPositive() {
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

    @Test
    fun getStockDisplay_packagesAndRemainder_showsMixed() {
        val medication = createMedication(
            currentStock = 34.0,
            packageUnit = "盒",
            dosageForm = "片",
            packageSize = 14.0
        )
        assertEquals("2盒6片", medication.getStockDisplay())
    }

    @Test
    fun getStockDisplay_packagesOnly_showsPackages() {
        val medication = createMedication(
            currentStock = 28.0,
            packageUnit = "盒",
            dosageForm = "片",
            packageSize = 14.0
        )
        assertEquals("2盒", medication.getStockDisplay())
    }

    @Test
    fun getStockDisplay_remainderOnly_showsRemainder() {
        val medication = createMedication(
            currentStock = 6.0,
            packageUnit = "盒",
            dosageForm = "片",
            packageSize = 14.0
        )
        assertEquals("6片", medication.getStockDisplay())
    }

    @Test
    fun getStockDisplay_zeroStock_showsZeroRemainder() {
        val medication = createMedication(
            currentStock = 0.0,
            packageUnit = "盒",
            dosageForm = "片",
            packageSize = 14.0
        )
        assertEquals("0片", medication.getStockDisplay())
    }

    @Test
    fun getStockDisplay_singlePackage_showsCorrectly() {
        val medication = createMedication(
            currentStock = 14.0,
            packageUnit = "盒",
            dosageForm = "片",
            packageSize = 14.0
        )
        assertEquals("1盒", medication.getStockDisplay())
    }

    @Test
    fun getStockDisplay_mlUnit_showsCorrectly() {
        val medication = createMedication(
            currentStock = 15.0,
            packageUnit = "瓶",
            dosageForm = "ml",
            packageSize = 10.0
        )
        assertEquals("1瓶5ml", medication.getStockDisplay())
    }

    @Test
    fun alert4DayDateTime_returnsCorrectTime() {
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

    @Test
    fun alert1DayDateTime_returnsCorrectTime() {
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

    @Test
    fun alertColorStatus_urgent_whenDaysLessOrEqualOne() {
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

    private fun createMedication(
        id: Long = 1L,
        genericName: String = "测试药品",
        brandName: String? = null,
        specification: String? = null,
        packageUnit: String = "盒",
        dosageForm: String = "片",
        packageSize: Double = 14.0,
        currentStock: Double = 28.0,
        frequencyType: FrequencyType = FrequencyType.EVERY_X_DAYS,
        frequencyValue: Int = 1,
        dailyDosage: Double = 1.0,
        startDate: LocalDate? = LocalDate.of(2026, 6, 1),
        isActive: Boolean = true,
        calendarEventId: Long? = null,
        createdAt: Long = Instant.parse("2026-06-01T00:00:00Z").toEpochMilli()
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
        isActive = isActive,
        calendarEventId = calendarEventId,
        createdAt = createdAt
    )
}
