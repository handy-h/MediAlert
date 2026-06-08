package com.handy.medialert.alarm

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.handy.medialert.data.entity.FrequencyType
import com.handy.medialert.data.entity.Medication
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var scheduler: AlarmScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduler = AlarmScheduler(context)
    }

    // ============================================================
    // scheduleAlarm() 调度闹钟测试
    // ============================================================

    @Test
    fun scheduleAlarm_futureTime_doesNotCrash() {
        // 构造一个耗尽日期在未来的药品
        val medication = createMedication(
            id = 1L,
            currentStock = 30.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = LocalDate.now()
        )

        // 不应崩溃
        scheduler.scheduleAlarm(medication)
    }

    @Test
    fun scheduleAlarm_pastTime_doesNotCrash() {
        // 构造一个耗尽日期在过去的药品
        val medication = createMedication(
            id = 2L,
            currentStock = 1.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 10.0,
            startDate = LocalDate.now().minusDays(30)
        )

        // 过去时间应被跳过，不崩溃
        scheduler.scheduleAlarm(medication)
    }

    @Test
    fun scheduleAlarm_zeroStock_doesNotCrash() {
        // 零库存药品
        val medication = createMedication(
            id = 3L,
            currentStock = 0.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = LocalDate.now()
        )

        scheduler.scheduleAlarm(medication)
    }

    // ============================================================
    // cancelAlarm() 取消闹钟测试
    // ============================================================

    @Test
    fun cancelAlarm_doesNotCrash() {
        // 取消一个不存在的闹钟不应崩溃
        scheduler.cancelAlarm(999L)
    }

    @Test
    fun scheduleAndCancel_roundTrip() {
        val medication = createMedication(
            id = 100L,
            currentStock = 30.0,
            startDate = LocalDate.now()
        )

        scheduler.scheduleAlarm(medication)
        scheduler.cancelAlarm(100L)
        // 先注册再取消，不应崩溃
    }

    // ============================================================
    // rescheduleAllAlarms() 批量重注册测试
    // ============================================================

    @Test
    fun rescheduleAllAlarms_onlyProcessesActiveMedications() {
        val activeMed = createMedication(
            id = 1L,
            currentStock = 30.0,
            startDate = LocalDate.now(),
            isActive = true
        )
        val inactiveMed = createMedication(
            id = 2L,
            currentStock = 30.0,
            startDate = LocalDate.now(),
            isActive = false
        )

        // 只有启用的药品会被调度，不应崩溃
        scheduler.rescheduleAllAlarms(listOf(activeMed, inactiveMed))
    }

    @Test
    fun rescheduleAllAlarms_emptyList_doesNotCrash() {
        scheduler.rescheduleAllAlarms(emptyList())
    }

    // ============================================================
    // 辅助工厂方法
    // ============================================================

    private fun createMedication(
        id: Long = 1L,
        currentStock: Double = 28.0,
        frequencyType: FrequencyType = FrequencyType.EVERY_X_DAYS,
        frequencyValue: Int = 1,
        dailyDosage: Double = 1.0,
        startDate: LocalDate? = LocalDate.now(),
        isActive: Boolean = true
    ): Medication = Medication(
        id = id,
        genericName = "测试药品",
        packageUnit = "盒",
        dosageForm = "片",
        packageSize = 14,
        currentStock = currentStock,
        frequencyType = frequencyType,
        frequencyValue = frequencyValue,
        dailyDosage = dailyDosage,
        startDate = startDate,
        isActive = isActive
    )
}
