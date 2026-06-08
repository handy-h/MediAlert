package com.handy.medialert.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handy.medialert.data.database.AppDatabase
import com.handy.medialert.data.entity.FrequencyType
import com.handy.medialert.data.entity.Medication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MedicationDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var medicationDao: MedicationDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        medicationDao = database.medicationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ============================================================
    // 插入与查询测试
    // ============================================================

    @Test
    fun insert_and_getById_returnsCorrectData() = runTest {
        val medication = createSampleMedication()
        val id = medicationDao.insert(medication)
        assertTrue(id > 0)

        val result = medicationDao.getById(id)
        assertNotNull(result)
        assertEquals("厄贝沙坦", result!!.genericName)
        assertEquals("安博维", result.brandName)
        assertEquals("80/12.5", result.specification)
        assertEquals(14, result.packageSize)
        assertEquals(28.0, result.currentStock, 0.001)
    }

    @Test
    fun getById_nonExisting_returnsNull() = runTest {
        val result = medicationDao.getById(999L)
        assertNull(result)
    }

    @Test
    fun insert_multiple_getAllActive_filtersCorrectly() = runTest {
        val active1 = createSampleMedication(genericName = "药品A", isActive = true)
        val active2 = createSampleMedication(genericName = "药品B", isActive = true)
        val inactive = createSampleMedication(genericName = "药品C", isActive = false)

        medicationDao.insert(active1)
        medicationDao.insert(active2)
        medicationDao.insert(inactive)

        val activeList = medicationDao.getAllActive().first()
        assertEquals(2, activeList.size)
        assertTrue(activeList.all { it.isActive })

        val inactiveList = medicationDao.getAllInactive().first()
        assertEquals(1, inactiveList.size)
        assertEquals("药品C", inactiveList[0].genericName)
    }

    @Test
    fun getAll_returnsAllMedications() = runTest {
        medicationDao.insert(createSampleMedication(genericName = "A"))
        medicationDao.insert(createSampleMedication(genericName = "B"))
        medicationDao.insert(createSampleMedication(genericName = "C", isActive = false))

        val all = medicationDao.getAll().first()
        assertEquals(3, all.size)
    }

    @Test
    fun getAllActiveSync_returnsOnlyActive() = runTest {
        medicationDao.insert(createSampleMedication(genericName = "A", isActive = true))
        medicationDao.insert(createSampleMedication(genericName = "B", isActive = false))

        val result = medicationDao.getAllActiveSync()
        assertEquals(1, result.size)
        assertEquals("A", result[0].genericName)
    }

    // ============================================================
    // 更新测试
    // ============================================================

    @Test
    fun update_modifiesExistingMedication() = runTest {
        val id = medicationDao.insert(createSampleMedication())
        val original = medicationDao.getById(id)!!

        val updated = original.copy(brandName = "新品牌", genericName = "新通用名")
        medicationDao.update(updated)

        val result = medicationDao.getById(id)
        assertEquals("新品牌", result!!.brandName)
        assertEquals("新通用名", result.genericName)
    }

    @Test
    fun updateStock_changesStockValue() = runTest {
        val id = medicationDao.insert(createSampleMedication(currentStock = 28.0))

        medicationDao.updateStock(id, 42.0)

        val result = medicationDao.getById(id)
        assertEquals(42.0, result!!.currentStock, 0.001)
    }

    @Test
    fun setActive_togglesToFalse() = runTest {
        val id = medicationDao.insert(createSampleMedication(isActive = true))

        medicationDao.setActive(id, false)

        val result = medicationDao.getById(id)
        assertFalse(result!!.isActive)
    }

    @Test
    fun setActive_togglesToTrue() = runTest {
        val id = medicationDao.insert(createSampleMedication(isActive = false))

        medicationDao.setActive(id, true)

        val result = medicationDao.getById(id)
        assertTrue(result!!.isActive)
    }

    // ============================================================
    // 删除测试
    // ============================================================

    @Test
    fun delete_removesMedication() = runTest {
        val id = medicationDao.insert(createSampleMedication())
        val medication = medicationDao.getById(id)!!

        medicationDao.delete(medication)

        val result = medicationDao.getById(id)
        assertNull(result)
    }

    // ============================================================
    // TypeConverter 数据库层面测试
    // ============================================================

    @Test
    fun localDate_storedAndRetrievedCorrectly() = runTest {
        val startDate = LocalDate.of(2026, 3, 15)
        val id = medicationDao.insert(createSampleMedication(startDate = startDate))

        val result = medicationDao.getById(id)
        assertEquals(startDate, result!!.startDate)
    }

    @Test
    fun localDate_null_storedAsNull() = runTest {
        val id = medicationDao.insert(createSampleMedication(startDate = null))

        val result = medicationDao.getById(id)
        assertNull(result!!.startDate)
    }

    // ============================================================
    // 辅助工厂方法
    // ============================================================

    private fun createSampleMedication(
        genericName: String = "厄贝沙坦",
        brandName: String? = "安博维",
        specification: String? = "80/12.5",
        currentStock: Double = 28.0,
        isActive: Boolean = true,
        startDate: LocalDate? = LocalDate.of(2026, 6, 1)
    ): Medication = Medication(
        genericName = genericName,
        brandName = brandName,
        specification = specification,
        packageUnit = "盒",
        dosageForm = "片",
        packageSize = 14,
        currentStock = currentStock,
        frequencyType = FrequencyType.EVERY_X_DAYS,
        frequencyValue = 1,
        dailyDosage = 1.0,
        startDate = startDate,
        isActive = isActive
    )
}
