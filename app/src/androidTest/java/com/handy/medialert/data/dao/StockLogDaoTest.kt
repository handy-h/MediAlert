package com.handy.medialert.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handy.medialert.data.database.AppDatabase
import com.handy.medialert.data.entity.FrequencyType
import com.handy.medialert.data.entity.Medication
import com.handy.medialert.data.entity.StockLog
import com.handy.medialert.data.entity.StockLogType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StockLogDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var medicationDao: MedicationDao
    private lateinit var stockLogDao: StockLogDao

    private var medicationId: Long = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        medicationDao = database.medicationDao()
        stockLogDao = database.stockLogDao()

        // 插入测试药品
        val medication = Medication(
            genericName = "测试药品",
            packageUnit = "盒",
            dosageForm = "片",
            packageSize = 14.0,
            currentStock = 28.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0
        )
        runTest { medicationId = medicationDao.insert(medication) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ============================================================
    // 插入与查询测试
    // ============================================================

    @Test
    fun insert_and_queryByMedication_returnsLogs() = runTest {
        val log = StockLog(
            medicationId = medicationId,
            type = StockLogType.PURCHASE,
            quantity = 14.0,
            reason = "采购补货"
        )
        stockLogDao.insert(log)

        val logs = stockLogDao.getLogsForMedication(medicationId).first()
        assertEquals(1, logs.size)
        assertEquals(StockLogType.PURCHASE, logs[0].type)
        assertEquals(14.0, logs[0].quantity, 0.001)
        assertEquals("采购补货", logs[0].reason)
    }

    @Test
    fun insert_multiple_returnsInDescendingTimestampOrder() = runTest {
        // 插入多条日志，验证按时间戳倒序返回
        stockLogDao.insert(StockLog(
            medicationId = medicationId,
            type = StockLogType.PURCHASE,
            quantity = 14.0,
            timestamp = 1000L
        ))
        stockLogDao.insert(StockLog(
            medicationId = medicationId,
            type = StockLogType.LOST,
            quantity = -3.0,
            timestamp = 2000L
        ))
        stockLogDao.insert(StockLog(
            medicationId = medicationId,
            type = StockLogType.ADJUSTMENT,
            quantity = 5.0,
            timestamp = 1500L
        ))

        val logs = stockLogDao.getLogsForMedication(medicationId).first()
        assertEquals(3, logs.size)
        // 按 timestamp 降序：2000, 1500, 1000
        assertEquals(2000L, logs[0].timestamp)
        assertEquals(1500L, logs[1].timestamp)
        assertEquals(1000L, logs[2].timestamp)
    }

    @Test
    fun queryByMedication_noLogs_returnsEmpty() = runTest {
        val logs = stockLogDao.getLogsForMedication(medicationId).first()
        assertTrue(logs.isEmpty())
    }

    // ============================================================
    // 级联删除测试
    // ============================================================

    @Test
    fun deleteLogsForMedication_removesAllLogsForThatMedication() = runTest {
        stockLogDao.insert(StockLog(
            medicationId = medicationId,
            type = StockLogType.PURCHASE,
            quantity = 14.0
        ))
        stockLogDao.insert(StockLog(
            medicationId = medicationId,
            type = StockLogType.LOST,
            quantity = -3.0
        ))

        // 删除该药品的所有日志
        stockLogDao.deleteLogsForMedication(medicationId)

        val logs = stockLogDao.getLogsForMedication(medicationId).first()
        assertTrue(logs.isEmpty())
    }

    @Test
    fun cascadeDelete_medicatioDeleted_removesAssociatedLogs() = runTest {
        // 插入药品和关联日志
        stockLogDao.insert(StockLog(
            medicationId = medicationId,
            type = StockLogType.PURCHASE,
            quantity = 14.0
        ))

        // 删除药品
        val medication = medicationDao.getById(medicationId)!!
        medicationDao.delete(medication)

        // 验证关联日志被级联删除（外键 CASCADE）
        val logs = stockLogDao.getLogsForMedication(medicationId).first()
        assertTrue(logs.isEmpty())
    }

    // ============================================================
    // 多药品日志隔离测试
    // ============================================================

    @Test
    fun logs_isolatedByMedicationId() = runTest {
        // 插入第二个药品
        val med2Id: Long = medicationDao.insert(Medication(
            genericName = "药品B",
            packageUnit = "盒",
            dosageForm = "片",
            packageSize = 14.0,
            currentStock = 14.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0
        ))

        stockLogDao.insert(StockLog(
            medicationId = medicationId,
            type = StockLogType.PURCHASE,
            quantity = 14.0
        ))
        stockLogDao.insert(StockLog(
            medicationId = med2Id,
            type = StockLogType.LOST,
            quantity = -3.0
        ))

        val logs1 = stockLogDao.getLogsForMedication(medicationId).first()
        val logs2 = stockLogDao.getLogsForMedication(med2Id).first()

        assertEquals(1, logs1.size)
        assertEquals(StockLogType.PURCHASE, logs1[0].type)

        assertEquals(1, logs2.size)
        assertEquals(StockLogType.LOST, logs2[0].type)
    }
}
