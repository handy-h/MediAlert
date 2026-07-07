package com.handy.medialert.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.handy.medialert.data.database.AppDatabase
import com.handy.medialert.reminder.ReminderManager
import com.handy.medialert.repository.MedicationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val database = AppDatabase.getDatabase(context)
        val repository = MedicationRepository(database.medicationDao(), database.stockLogDao())
        val alarmScheduler = AlarmScheduler(context)
        val reminderManager = ReminderManager(context, repository, alarmScheduler)

        // 使用 SupervisorJob 确保子协程异常不会影响其他协程
        // 使用 Dispatchers.IO 执行数据库操作
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val medications = database.medicationDao().getAllActiveSync()
                // 先恢复闹钟（快速恢复，不依赖日历权限）
                alarmScheduler.rescheduleAllAlarms(medications)
                // 再尝试恢复完整提醒（日历事件 + 闹钟）
                // 日历事件通常在重启后仍然存在，但如果有缺失会重新创建
                val prefs = context.getSharedPreferences("medialert_prefs", Context.MODE_PRIVATE)
                val calendarId = prefs.getLong("calendar_id", -1L).takeIf { it >= 0 }
                reminderManager.refreshAllReminders(medications, calendarId)
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "重启后重新注册提醒失败", e)
            } finally {
                pendingResult.finish()
                // 显式取消协程作用域，避免进程被系统回收前作用域长期持有资源
                scope.cancel()
            }
        }
    }
}
