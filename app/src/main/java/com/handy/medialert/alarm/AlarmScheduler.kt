package com.handy.medialert.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.handy.medialert.data.entity.Medication
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(medication: Medication) {
        val alertTime = medication.alert1DayDateTime()
        if (alertTime.isBefore(LocalDateTime.now())) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("medication_id", medication.id)
            putExtra("medication_name", medication.genericName)
            putExtra("alert_type", "1day")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            generateRequestCode(medication.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = alertTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // 精确闹钟权限未授予，使用非精确闹钟作为降级方案
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                return
            }
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.w("AlarmScheduler", "精确闹钟权限不足，降级为非精确闹钟", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(medicationId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            generateRequestCode(medicationId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun rescheduleAllAlarms(medications: List<Medication>) {
        medications.filter { it.isActive }.forEach { medication ->
            cancelAlarm(medication.id)
            scheduleAlarm(medication)
        }
    }

    /**
     * 生成安全的 PendingIntent requestCode
     * 使用 hashCode 避免 Long 转 Int 的溢出问题
     */
    private fun generateRequestCode(medicationId: Long): Int {
        return medicationId.hashCode()
    }
}
