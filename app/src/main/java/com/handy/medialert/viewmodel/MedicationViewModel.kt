package com.handy.medialert.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.handy.medialert.MediAlertApplication
import com.handy.medialert.data.entity.FrequencyType
import com.handy.medialert.data.entity.Medication
import com.handy.medialert.reminder.ReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 药品 ViewModel
 * 负责 UI 状态管理和用户操作响应
 * 提醒相关逻辑已委托给 ReminderManager
 */
class MedicationViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MediAlertApplication
    private val repository = app.repository
    private val reminderManager = ReminderManager(application, app.repository)

    val activeMedications: StateFlow<List<Medication>> = repository.getAllActiveMedications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inactiveMedications: StateFlow<List<Medication>> = repository.getAllInactiveMedications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 添加新药品
     */
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
            reminderManager.registerReminders(savedMedication)
        }
    }

    /**
     * 更新药品信息
     */
    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            repository.updateMedication(medication)
        }
    }

    /**
     * 删除药品（会自动取消关联提醒）
     */
    fun deleteMedication(medicationId: Long) {
        viewModelScope.launch {
            val medication = repository.getMedicationById(medicationId) ?: return@launch
            reminderManager.cancelReminders(medication)
            repository.deleteMedication(medication)
        }
    }

    /**
     * 获取药品 Flow
     */
    fun getMedicationFlow(id: Long): Flow<Medication?> =
        flow { emit(repository.getMedicationById(id)) }

    /**
     * 增加库存
     */
    fun addStock(medicationId: Long, quantity: Double, reason: String?) {
        viewModelScope.launch {
            repository.addStock(medicationId, quantity, reason)
            val medication = repository.getMedicationById(medicationId) ?: return@launch
            // 重新注册提醒（库存变化可能影响耗尽日期）
            reminderManager.registerReminders(medication)
        }
    }

    /**
     * 减少库存
     */
    fun reduceStock(medicationId: Long, quantity: Double, reason: String?) {
        viewModelScope.launch {
            repository.reduceStock(medicationId, quantity, reason)
            val medication = repository.getMedicationById(medicationId) ?: return@launch
            // 重新注册提醒（库存变化可能影响耗尽日期）
            reminderManager.registerReminders(medication)
        }
    }

    /**
     * 停用药品
     */
    fun deactivateMedication(medicationId: Long) {
        viewModelScope.launch {
            val medication = repository.getMedicationById(medicationId) ?: return@launch
            reminderManager.cancelReminders(medication)
            repository.setMedicationActive(medicationId, false)
        }
    }

    /**
     * 重新启用药品
     */
    fun activateMedication(medicationId: Long) {
        viewModelScope.launch {
            repository.setMedicationActive(medicationId, true)
            val medication = repository.getMedicationById(medicationId) ?: return@launch
            reminderManager.registerReminders(medication)
        }
    }

    /**
     * 刷新所有提醒（选择日历后调用）
     */
    fun refreshAllReminders(calendarId: Long) {
        viewModelScope.launch {
            reminderManager.refreshAllReminders(activeMedications.value, calendarId)
        }
    }

    /**
     * 导出 CSV
     */
    suspend fun exportToCsv(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val medications = activeMedications.value
                if (medications.isEmpty()) return@withContext null

                val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
                    ?: context.filesDir
                val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    .format(java.time.LocalDateTime.now())
                val fileName = "药箱库存_$timestamp.csv"
                val file = File(dir, fileName)

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
}
