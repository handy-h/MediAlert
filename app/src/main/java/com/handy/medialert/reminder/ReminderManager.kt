package com.handy.medialert.reminder

import android.content.Context
import com.handy.medialert.alarm.AlarmScheduler
import com.handy.medialert.calendar.CalendarManager
import com.handy.medialert.data.entity.Medication
import com.handy.medialert.repository.MedicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 提醒管理器
 * 负责协调闹钟提醒和日历提醒的注册/取消
 * 解耦 ViewModel 与系统服务的直接依赖
 */
class ReminderManager(
    private val context: Context,
    private val repository: MedicationRepository
) {
    private val alarmScheduler = AlarmScheduler(context)
    private val calendarManager = CalendarManager(context)

    /**
     * 为药品注册提醒（闹钟 + 日历）
     * @param medication 药品信息
     * @param calendarId 日历账户ID（可选）
     * @return 更新后的药品（包含 calendarEventId）
     */
    suspend fun registerReminders(
        medication: Medication,
        calendarId: Long? = null
    ): Medication = withContext(Dispatchers.IO) {
        if (!medication.isActive || medication.daysUntilDepletion() <= 0) {
            return@withContext medication
        }

        // 取消旧提醒
        cancelReminders(medication)

        // 注册日历事件（提前4天）
        val newEventId = calendarId?.let {
            calendarManager.addMedicationAlert(it, medication, medication.calendarEventId)
        }

        // 注册闹钟（提前1天）
        alarmScheduler.scheduleAlarm(medication)

        // 如果生成了新的事件ID，更新数据库
        return@withContext if (newEventId != null && newEventId != medication.calendarEventId) {
            val updated = medication.copy(calendarEventId = newEventId)
            repository.updateMedication(updated)
            updated
        } else {
            medication
        }
    }

    /**
     * 取消药品的所有提醒
     * @param medication 药品信息
     */
    suspend fun cancelReminders(medication: Medication) = withContext(Dispatchers.IO) {
        // 取消闹钟
        alarmScheduler.cancelAlarm(medication.id)

        // 取消日历事件
        medication.calendarEventId?.let { eventId ->
            calendarManager.deleteEvent(eventId)
            // 清除事件ID
            val updated = medication.copy(calendarEventId = null)
            repository.updateMedication(updated)
        }
    }

    /**
     * 刷新所有活跃药品的提醒
     * @param medications 药品列表
     * @param calendarId 日历账户ID
     */
    suspend fun refreshAllReminders(
        medications: List<Medication>,
        calendarId: Long
    ) = withContext(Dispatchers.IO) {
        medications.filter { it.isActive }.forEach { medication ->
            registerReminders(medication, calendarId)
        }
    }

    /**
     * 设备重启后重新注册所有提醒
     * @param medications 活跃药品列表
     */
    fun rescheduleAllAlarms(medications: List<Medication>) {
        alarmScheduler.rescheduleAllAlarms(medications)
    }
}
