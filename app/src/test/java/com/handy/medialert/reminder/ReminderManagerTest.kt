package com.handy.medialert.reminder

import android.content.Context
import com.handy.medialert.TestMedications
import com.handy.medialert.alarm.AlarmScheduler
import com.handy.medialert.calendar.CalendarManager
import com.handy.medialert.repository.MedicationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.handy.medialert.data.entity.Medication

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderManagerTest {

    private data class Harness(
        val manager: ReminderManager,
        val context: Context,
        val repo: MedicationRepository,
        val alarmScheduler: AlarmScheduler,
        val calendarManager: CalendarManager
    )

    private fun buildManager(): Harness {
        val context = mockk<Context>(relaxed = true)
        val repo = mockk<MedicationRepository>(relaxed = true)
        val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
        val calendarManager = mockk<CalendarManager>(relaxed = true)
        val manager = ReminderManager(context, repo, alarmScheduler, calendarManager)
        return Harness(manager, context, repo, alarmScheduler, calendarManager)
    }

    @Test
    fun `registerReminders 跳过停用的药品`() = runTest {
        val (manager, _, _, alarmScheduler, calendarManager) = buildManager()
        val med = TestMedications.sample(isActive = false)

        manager.registerReminders(med)

        coVerify(exactly = 0) { alarmScheduler.scheduleAlarm(any()) }
        coVerify(exactly = 0) { calendarManager.addMedicationAlert(any(), any(), any()) }
    }

    @Test
    fun `registerReminders 跳过已耗尽的药品`() = runTest {
        val (manager, _, _, alarmScheduler, calendarManager) = buildManager()
        // currentStock = 0 -> 耗尽日期为今天 -> daysUntilDepletion <= 0
        val med = TestMedications.sample(currentStock = 0.0)

        manager.registerReminders(med)

        coVerify(exactly = 0) { alarmScheduler.scheduleAlarm(any()) }
        coVerify(exactly = 0) { calendarManager.addMedicationAlert(any(), any(), any()) }
    }

    @Test
    fun `registerReminders 活跃药品会取消旧提醒并注册新提醒`() = runTest {
        val (manager, _, _, alarmScheduler, calendarManager) = buildManager()
        val med = TestMedications.sample(id = 7L, calendarEventId = null)
        coEvery { calendarManager.addMedicationAlert(10L, med, null) } returns 999L

        manager.registerReminders(med, 10L)

        // 先取消旧闹钟，再注册新闹钟 + 日历事件
        coVerifyOrder {
            alarmScheduler.cancelAlarm(7L)
            alarmScheduler.scheduleAlarm(med)
        }
        coVerify { calendarManager.addMedicationAlert(10L, med, null) }
    }

    @Test
    fun `registerReminders 生成新日历事件ID时写回数据库`() = runTest {
        val (manager, _, repo, _, calendarManager) = buildManager()
        val med = TestMedications.sample(calendarEventId = null)
        coEvery { calendarManager.addMedicationAlert(10L, med, null) } returns 999L

        manager.registerReminders(med, 10L)

        val captor = slot<Medication>()
        coVerify { repo.updateMedication(capture(captor)) }
        assertEquals(999L, captor.captured.calendarEventId)
    }

    @Test
    fun `cancelReminders 取消闹钟并删除日历事件、清除事件Id`() = runTest {
        val (manager, _, repo, alarmScheduler, calendarManager) = buildManager()
        val med = TestMedications.sample(id = 8L, calendarEventId = 123L)

        manager.cancelReminders(med)

        coVerify { alarmScheduler.cancelAlarm(8L) }
        coVerify { calendarManager.deleteEvent(123L) }
        val captor = slot<Medication>()
        coVerify { repo.updateMedication(capture(captor)) }
        assertNull(captor.captured.calendarEventId)
    }
}
