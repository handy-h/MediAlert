package com.handy.medialert.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.handy.medialert.MainActivity
import com.handy.medialert.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra("medication_id", -1)
        val medicationName = intent.getStringExtra("medication_name") ?: "药品"
        val alertType = intent.getStringExtra("alert_type") ?: "1day"

        if (medicationId == -1L) return

        showNotification(context, medicationId, medicationName, alertType)
    }

    private fun showNotification(
        context: Context,
        medicationId: Long,
        medicationName: String,
        alertType: String
    ) {
        val channelId = "medialert_alarms"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("medication_id", medicationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            medicationId.toInt(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (alertType) {
            "1day" -> context.getString(R.string.notification_title_1day)
            else -> context.getString(R.string.notification_title_generic)
        }

        val content = context.getString(R.string.notification_content, medicationName)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(medicationId.toInt(), notification)
    }
}
