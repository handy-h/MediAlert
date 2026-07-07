package com.handy.medialert.ui.screens

import com.handy.medialert.TestMedications
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MergedAlertsTest {

    @Test
    fun `空列表返回空 Map`() {
        assertTrue(calculateMergedAlerts(emptyList()).isEmpty())
    }

    @Test
    fun `活跃且未耗尽的药品按 提前4天 分组`() {
        val med = TestMedications.sample(id = 1L)
        val result = calculateMergedAlerts(listOf(med))

        assertEquals(1, result.size)
        val entry = result.entries.first()
        // 提醒日期 = 耗尽日期 - 4 天
        assertEquals(med.depletionDate().minusDays(4), entry.key)
        assertTrue(entry.value.contains(med))
    }

    @Test
    fun `停用的药品被排除`() {
        val med = TestMedications.sample(isActive = false)
        assertFalse(calculateMergedAlerts(listOf(med)).containsKey(med.depletionDate().minusDays(4)))
        assertTrue(calculateMergedAlerts(listOf(med)).isEmpty())
    }

    @Test
    fun `已耗尽的药品被排除`() {
        // currentStock = 0 -> 当天耗尽 -> daysUntilDepletion <= 0
        val med = TestMedications.sample(currentStock = 0.0)
        assertTrue(calculateMergedAlerts(listOf(med)).isEmpty())
    }

    @Test
    fun `3天内的提醒被合并到同一组`() {
        // 两个药品的耗尽日期相差 2 天 -> 提醒日期相差 2 天 (< 4) -> 合并为一组
        val base = LocalDate.now().plusDays(20)
        val medA = TestMedications.sample(id = 1L, startDate = base.minusDays(10), currentStock = 10.0, dailyDosage = 1.0)
        val medB = TestMedications.sample(id = 2L, startDate = base.minusDays(8), currentStock = 10.0, dailyDosage = 1.0)
        // medA 耗尽 = base-10 + 10 = base; medB 耗尽 = base-8 + 10 = base+2
        // 提醒日期: base-4 与 base-2，相差 2 天 -> 合并
        val result = calculateMergedAlerts(listOf(medA, medB))
        assertEquals(1, result.size)
        assertEquals(2, result.values.first().size)
    }
}
