package com.handy.medialert.calendar

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.handy.medialert.R
import com.handy.medialert.data.entity.Medication
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

class CalendarManager(private val context: Context) {

    fun getCalendars(): List<CalendarInfo> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val calendars = mutableListOf<CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME
        )

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val displayName = cursor.getString(1) ?: context.getString(R.string.unnamed_calendar)
                val accountName = cursor.getString(2) ?: ""
                calendars.add(CalendarInfo(id, displayName, accountName))
            }
        }
        return calendars
    }

    /**
     * 添加药品提醒日历事件
     * @param calendarId 日历账户ID
     * @param medication 药品信息
     * @param existingEventId 已存在的事件ID（用于更新），为null则创建新事件
     * @return 创建/更新后的事件ID
     */
    fun addMedicationAlert(calendarId: Long, medication: Medication, existingEventId: Long? = null): Long? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val alertTime = medication.alert4DayDateTime()
        if (alertTime.isBefore(LocalDateTime.now())) return null

        val startMillis = alertTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = startMillis + 60 * 60 * 1000 // 1小时事件

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, context.getString(R.string.calendar_event_title, medication.genericName))
            put(CalendarContract.Events.DESCRIPTION, buildDescription(medication))
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
            put(CalendarContract.Events.RRULE, null as String?) // 一次性事件
        }

        // 如果已存在事件ID，先删除旧事件再创建新事件（更新场景）
        existingEventId?.let { deleteEvent(it) }

        val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        val eventId = uri?.lastPathSegment?.toLongOrNull()

        // 添加提醒（提前0分钟，即事件开始时）
        eventId?.let { addReminder(it) }

        return eventId
    }

    private fun addReminder(eventId: Long) {
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, 0)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
    }

    fun deleteEvent(eventId: Long) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val deleteUri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, eventId.toString())
        context.contentResolver.delete(deleteUri, null, null)
    }

    private fun buildDescription(medication: Medication): String {
        val notFilled = context.getString(R.string.not_filled)
        return buildString {
            medication.brandName?.let { appendLine(context.getString(R.string.calendar_desc_brand, it)) }
            appendLine(context.getString(R.string.calendar_desc_spec, medication.specification ?: notFilled))
            appendLine(context.getString(R.string.calendar_desc_stock, medication.getStockDisplay()))
            appendLine(context.getString(R.string.calendar_desc_depletion, medication.depletionDate().toString()))
            appendLine(context.getString(R.string.calendar_desc_reminder))
        }
    }

    data class CalendarInfo(
        val id: Long,
        val displayName: String,
        val accountName: String
    )
}
