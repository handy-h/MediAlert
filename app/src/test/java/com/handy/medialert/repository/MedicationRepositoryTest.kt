package com.handy.medialert.repository

import android.content.Context
import com.handy.medialert.data.dao.MedicationDao
import com.handy.medialert.data.dao.StockLogDao
import com.handy.medialert.data.entity.FrequencyType
import com.handy.medialert.data.entity.Medication
import com.handy.medialert.data.entity.StockLog
import com.handy.medialert.data.entity.StockLogType
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MedicationRepositoryTest {

    private lateinit var medicationDao: MedicationDao
    private lateinit var stockLogDao: StockLogDao
    private lateinit var context: Context
    private lateinit var repository: MedicationRepository

    private val sampleMedication = Medication(
        id = 1L,
        genericName = "厄贝沙坦",
        brandName = "安博维",
        specification = "80/12.5",
        packageUnit = "盒",
        dosageForm = "片",
        packageSize = 14,
        currentStock = 28.0,
        frequencyType = FrequencyType.EVERY_X_DAYS,
        frequencyValue = 1,
        dailyDosage = 1.0,
        isActive = true
    )

    @Before
    fun setUp() {
        medicationDao = mockk(relaxed = true)
        stockLogDao = mockk(relaxed = true)
        context = mockk(relaxed = true)
        repository = MedicationRepository(medicationDao, stockLogDao, context)
    }

    // ============================================================
    // 查询操作测试
    // ============================================================

    @Test
    fun getAllActiveMedications_delegatesToDao() = runTest {
        val expected = listOf(sampleMedication)
        every { medicationDao.getAllActive() } returns flowOf(expected)

        val result = repository.getAllActiveMedications().first()
        assertEquals(expected, result)
        verify { medicationDao.getAllActive() }
    }

    @Test
    fun getAllInactiveMedications_delegatesToDao() = runTest {
        val expected = emptyList<Medication>()
        every { medicationDao.getAllInactive() } returns flowOf(expected)

        val result = repository.getAllInactiveMedications().first()
        assertEquals(expected, result)
        verify { medicationDao.getAllInactive() }
    }

    @Test
    fun getAllMedications_delegatesToDao() = runTest {
        val expected = listOf(sampleMedication)
        every { medicationDao.getAll() } returns flowOf(expected)

        val result = repository.getAllMedications().first()
        assertEquals(expected, result)
        verify { medicationDao.getAll() }
    }

    @Test
    fun getMedicationById_existingMedication_returnsMedication() = runTest {
        coEvery { medicationDao.getById(1L) } returns sampleMedication

        val result = repository.getMedicationById(1L)
        assertEquals(sampleMedication, result)
    }

    @Test
    fun getMedicationById_nonExisting_returnsNull() = runTest {
        coEvery { medicationDao.getById(999L) } returns null

        val result = repository.getMedicationById(999L)
        assertNull(result)
    }

    // ============================================================
    // 添加药品测试
    // ============================================================

    @Test
    fun addMedication_insertsAndReturnsId() = runTest {
        coEvery { medicationDao.insert(any()) } returns 1L

        val result = repository.addMedication(sampleMedication)
        assertEquals(1L, result)
        coVerify { medicationDao.insert(sampleMedication) }
    }

    // ============================================================
    // 修改药品测试
    // ============================================================

    @Test
    fun updateMedication_delegatesToDao() = runTest {
        val updated = sampleMedication.copy(brandName = "新商品名")

        repository.updateMedication(updated)
        coVerify { medicationDao.update(updated) }
    }

    @Test
    fun deleteMedication_delegatesToDao() = runTest {
        repository.deleteMedication(sampleMedication)
        coVerify { medicationDao.delete(sampleMedication) }
    }

    // ============================================================
    // 补货操作测试
    // ============================================================

    @Test
    fun addStock_updatesStockAndLogsPurchase() = runTest {
        coEvery { medicationDao.getById(1L) } returns sampleMedication

        repository.addStock(1L, 14.0, "采购")

        // 验证库存更新: 28 + 14 = 42
        coVerify { medicationDao.updateStock(1L, 42.0) }

        // 验证日志记录
        coVerify {
            stockLogDao.insert(match<StockLog> {
                it.medicationId == 1L &&
                    it.type == StockLogType.PURCHASE &&
                    it.quantity == 14.0 &&
                    it.reason == "采购"
            })
        }
    }

    @Test
    fun addStock_medicatioNotFound_doesNothing() = runTest {
        coEvery { medicationDao.getById(999L) } returns null

        repository.addStock(999L, 14.0)

        coVerify(exactly = 0) { medicationDao.updateStock(any(), any()) }
        coVerify(exactly = 0) { stockLogDao.insert(any()) }
    }

    // ============================================================
    // 消耗操作测试
    // ============================================================

    @Test
    fun reduceStock_updatesStockAndLogsLost() = runTest {
        coEvery { medicationDao.getById(1L) } returns sampleMedication

        repository.reduceStock(1L, 5.0, "丢失")

        // 验证库存更新: 28 - 5 = 23
        coVerify { medicationDao.updateStock(1L, 23.0) }

        // 验证日志记录（数量为负数）
        coVerify {
            stockLogDao.insert(match<StockLog> {
                it.medicationId == 1L &&
                    it.type == StockLogType.LOST &&
                    it.quantity == -5.0 &&
                    it.reason == "丢失"
            })
        }
    }

    @Test
    fun reduceStock_exceedingStock_coercesToZero() = runTest {
        coEvery { medicationDao.getById(1L) } returns sampleMedication

        // 消耗量超过库存: 28 - 100 = -72 → coerceAtLeast(0.0) → 0
        repository.reduceStock(1L, 100.0)

        coVerify { medicationDao.updateStock(1L, 0.0) }
    }

    @Test
    fun reduceStock_medicatioNotFound_doesNothing() = runTest {
        coEvery { medicationDao.getById(999L) } returns null

        repository.reduceStock(999L, 5.0)

        coVerify(exactly = 0) { medicationDao.updateStock(any(), any()) }
        coVerify(exactly = 0) { stockLogDao.insert(any()) }
    }

    // ============================================================
    // 状态切换测试
    // ============================================================

    @Test
    fun setMedicationActive_deactivate_delegatesToDao() = runTest {
        repository.setMedicationActive(1L, false)
        coVerify { medicationDao.setActive(1L, false) }
    }

    @Test
    fun setMedicationActive_reactivate_delegatesToDao() = runTest {
        repository.setMedicationActive(1L, true)
        coVerify { medicationDao.setActive(1L, true) }
    }
}
