package com.handy.medialert.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
            medication.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = alertTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancelAlarm(medicationId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.toInt(),
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
}
