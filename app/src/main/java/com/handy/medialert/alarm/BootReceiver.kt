package com.handy.medialert.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.handy.medialert.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val database = AppDatabase.getDatabase(context)
            val alarmScheduler = AlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                database.medicationDao().getAll().collect { medications ->
                    alarmScheduler.rescheduleAllAlarms(medications.filter { it.isActive })
                }
            }
        }
    }
}
