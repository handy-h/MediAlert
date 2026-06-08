package com.handy.medialert.viewmodel

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.handy.medialert.alarm.AlarmScheduler
import com.handy.medialert.calendar.CalendarManager
import com.handy.medialert.data.database.AppDatabase
import com.handy.medialert.data.entity.FrequencyType
import com.handy.medialert.data.entity.Medication
import com.handy.medialert.repository.MedicationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MedicationViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = MedicationRepository(
        database.medicationDao(),
        database.stockLogDao(),
        application
    )
    private val alarmScheduler = AlarmScheduler(application)
    private val calendarManager = CalendarManager(application)

    val activeMedications: StateFlow<List<Medication>> = repository.getAllActiveMedications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inactiveMedications: StateFlow<List<Medication>> = repository.getAllInactiveMedications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMedication(
        genericName: String,
        brandName: String?,
        specification: String?,
        packageUnit: String,
        dosageForm: String,
        packageSize: Int,
        currentStock: Double,
        frequencyType: FrequencyType,
        frequencyValue: Int,
        dailyDosage: Double,
        startDate: LocalDate?
    ) {
        viewModelScope.launch {
            val medication = Medication(
                genericName = genericName,
                brandName = brandName,
                specification = specification,
                packageUnit = packageUnit,
                dosageForm = dosageForm,
                packageSize = packageSize,
                currentStock = currentStock,
                frequencyType = frequencyType,
                frequencyValue = frequencyValue,
                dailyDosage = dailyDosage,
                startDate = startDate
            )
            val id = repository.addMedication(medication)
            val savedMedication = repository.getMedicationById(id) ?: return@launch

            // 注册提醒
            registerReminders(savedMedication)
        }
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            repository.updateMedication(medication)
        }
    }

    fun deleteMedication(medicationId: Long) {
        viewModelScope.launch {
            val medication = repository.getMedicationById(medicationId) ?: return@launch
            cancelReminders(medication)
            repository.deleteMedication(medication)
        }
    }

    fun getMedicationById(id: Long): Medication? {
        var result: Medication? = null
        viewModelScope.launch {
            result = repository.getMedicationById(id)
        }
        return result
    }

    fun addStock(medicationId: Long, quantity: Double, reason: String?) {
        viewModelScope.launch {
            repository.addStock(medicationId, quantity, reason)
            val medication = repository.getMedicationById(medicationId) ?: return@launch
            // 重新注册提醒
            cancelReminders(medication)
            registerReminders(medication)
        }
    }

    fun reduceStock(medicationId: Long, quantity: Double, reason: String?) {
        viewModelScope.launch {
            repository.reduceStock(medicationId, quantity, reason)
            val medication = repository.getMedicationById(medicationId) ?: return@launch
            // 重新注册提醒
            cancelReminders(medication)
            registerReminders(medication)
        }
    }

    fun deactivateMedication(medicationId: Long) {
        viewModelScope.launch {
            repository.setMedicationActive(medicationId, false)
            val medication = repository.getMedicationById(medicationId) ?: return@launch
            cancelReminders(medication)
        }
    }

    fun activateMedication(medicationId: Long) {
        viewModelScope.launch {
            repository.setMedicationActive(medicationId, true)
            val medication = repository.getMedicationById(medicationId) ?: return@launch
            registerReminders(medication)
        }
    }

    fun refreshAllReminders(calendarId: Long) {
        viewModelScope.launch {
            activeMedications.value.forEach { medication ->
                cancelReminders(medication)
                registerReminders(medication, calendarId)
            }
        }
    }

    private fun registerReminders(medication: Medication, calendarId: Long? = null) {
        if (!medication.isActive) return
        if (medication.daysUntilDepletion() <= 0) return

        // 注册日历事件（提前4天）
        calendarId?.let {
            calendarManager.addMedicationAlert(it, medication)
        }

        // 注册闹钟（提前1天）
        alarmScheduler.scheduleAlarm(medication)
    }

    private fun cancelReminders(medication: Medication) {
        alarmScheduler.cancelAlarm(medication.id)
        // 注意：日历事件取消需要存储eventId，这里简化处理
    }

    fun exportToCsv(context: Context): String? {
        return try {
            val medications = activeMedications.value
            if (medications.isEmpty()) return null

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "药箱库存_${System.currentTimeMillis()}.csv"
            val file = File(downloadsDir, fileName)

            CSVWriter(FileWriter(file)).use { writer ->
                // 表头
                writer.writeNext(arrayOf(
                    "通用名", "商品名", "规格", "包装", "剂型", "每包装数量",
                    "当前库存", "用药频率", "耗尽日期", "状态"
                ))

                // 数据
                medications.forEach { med ->
                    val freqText = when (med.frequencyType) {
                        FrequencyType.EVERY_X_DAYS -> "每${med.frequencyValue}天${med.dailyDosage}${med.dosageForm}"
                        FrequencyType.EVERY_XTH_DAY -> "每隔${med.frequencyValue}天${med.dailyDosage}${med.dosageForm}"
                    }
                    writer.writeNext(arrayOf(
                        med.genericName,
                        med.brandName ?: "",
                        med.specification ?: "",
                        med.packageUnit,
                        med.dosageForm,
                        med.packageSize.toString(),
                        med.getStockDisplay(),
                        freqText,
                        med.depletionDate().toString(),
                        if (med.isActive) "启用" else "停用"
                    ))
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
