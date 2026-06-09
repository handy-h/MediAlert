package com.handy.medialert.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.handy.medialert.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val database = AppDatabase.getDatabase(context)
        val alarmScheduler = AlarmScheduler(context)

        // 使用 SupervisorJob 确保子协程异常不会影响其他协程
        // 使用 Dispatchers.IO 执行数据库操作
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val medications = database.medicationDao().getAllActiveSync()
                alarmScheduler.rescheduleAllAlarms(medications)
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "重启后重新注册闹钟失败", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
