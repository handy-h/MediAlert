package com.handy.medialert.alarm

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import com.handy.medialert.data.entity.FrequencyType
import com.handy.medialert.data.entity.Medication
import io.mockk.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var scheduler: AlarmScheduler

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        alarmManager = mockk(relaxed = true)

        // Mock Intent 创建（避免 Android 框架依赖）
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().putExtra(any<String>(), any<Long>()) } returns mockk(relaxed = true)
        every { anyConstructed<Intent>().putExtra(any<String>(), any<String>()) } returns mockk(relaxed = true)

        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        every { alarmManager.canScheduleExactAlarms() } returns true

        scheduler = AlarmScheduler(context)
    }

    // ============================================================
    // scheduleAlarm() 调度闹钟测试
    // ============================================================

    @Test
    fun scheduleAlarm_futureTime_schedulesExactAlarm() {
        // 构造一个耗尽日期在未来的药品
        // stock=30, dailyDosage=1.0, everyDay → 30天后耗尽
        // alert1Day = 耗尽日-1天 = 29天后 → 是未来时间
        val medication = createMedication(
            id = 1L,
            currentStock = 30.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = LocalDate.now()
        )

        scheduler.scheduleAlarm(medication)

        verify { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
    }

    @Test
    fun scheduleAlarm_pastTime_skipsScheduling() {
        // 构造一个耗尽日期在过去的药品
        // stock=1, dailyDosage=10.0, everyDay → 0天后耗尽（今天）
        // alert1Day = 今天-1天 = 昨天 → 是过去时间，应跳过
        val medication = createMedication(
            id = 2L,
            currentStock = 1.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 10.0,
            startDate = LocalDate.now().minusDays(30)
        )

        scheduler.scheduleAlarm(medication)

        verify(exactly = 0) { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
        verify(exactly = 0) { alarmManager.set(any(), any(), any()) }
    }

    @Test
    fun scheduleAlarm_noExactAlarmPermission_fallsBackToInexact() {
        every { alarmManager.canScheduleExactAlarms() } returns false

        val medication = createMedication(
            id = 3L,
            currentStock = 30.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = LocalDate.now()
        )

        scheduler.scheduleAlarm(medication)

        // 降级使用非精确闹钟
        verify { alarmManager.set(any(), any(), any()) }
        verify(exactly = 0) { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
    }

    @Test
    fun scheduleAlarm_securityException_fallsBackToInexact() {
        every { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) } throws SecurityException("test")

        val medication = createMedication(
            id = 4L,
            currentStock = 30.0,
            frequencyType = FrequencyType.EVERY_X_DAYS,
            frequencyValue = 1,
            dailyDosage = 1.0,
            startDate = LocalDate.now()
        )

        scheduler.scheduleAlarm(medication)

        // 异常后降级使用非精确闹钟
        verify { alarmManager.set(any(), any(), any()) }
    }

    // ============================================================
    // cancelAlarm() 取消闹钟测试
    // ============================================================

    @Test
    fun cancelAlarm_cancelsPendingIntent() {
        scheduler.cancelAlarm(1L)

        verify { alarmManager.cancel(any<android.app.PendingIntent>()) }
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

        scheduler.rescheduleAllAlarms(listOf(activeMed, inactiveMed))

        // 只有启用的药品会被调度（先cancel再schedule）
        // inactive 被 filter 过滤掉了
        verify(exactly = 1) { alarmManager.cancel(any<android.app.PendingIntent>()) }
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
