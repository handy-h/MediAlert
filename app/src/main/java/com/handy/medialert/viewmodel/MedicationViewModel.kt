package com.handy.medialert.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.handy.medialert.R
import com.handy.medialert.MediAlertApplication
import com.handy.medialert.data.entity.FrequencyType
import com.handy.medialert.data.entity.Medication
import com.handy.medialert.reminder.ReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri
import android.util.Log
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
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
    private val prefs = application.getSharedPreferences("medialert_prefs", Context.MODE_PRIVATE)

    /** 获取持久化的日历账户ID */
    fun getSavedCalendarId(): Long? {
        val id = prefs.getLong("calendar_id", -1L)
        return if (id >= 0) id else null
    }

    /** 保存日历账户ID */
    fun saveCalendarId(calendarId: Long) {
        prefs.edit().putLong("calendar_id", calendarId).apply()
    }

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
        packageSize: Double,
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

            // 注册提醒（传入已保存的日历ID）
            reminderManager.registerReminders(savedMedication, getSavedCalendarId())
        }
    }

    /**
     * 更新药品信息
     * 更新后会自动重新注册提醒（用药频率/库存变化可能影响耗尽日期）
     */
    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            repository.updateMedication(medication)
            // 重新注册提醒（剂量/频率变化可能影响提醒时间）
            if (medication.isActive) {
                reminderManager.registerReminders(medication, getSavedCalendarId())
            }
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
            reminderManager.registerReminders(medication, getSavedCalendarId())
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
            reminderManager.registerReminders(medication, getSavedCalendarId())
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
            reminderManager.registerReminders(medication, getSavedCalendarId())
        }
    }

    /**
     * 刷新所有提醒（选择日历后调用）
     */
    fun refreshAllReminders(calendarId: Long) {
        viewModelScope.launch {
            val activeMeds = repository.getAllActiveMedications().first()
            reminderManager.refreshAllReminders(activeMeds, calendarId)
        }
    }

    /**
     * 重设所有提醒
     * 删除所有已注册的日历事件和闹钟，按当前库存重新设置
     * 使用已保存的日历账户ID，不影响其他 App 的日历事件
     */
    fun resetAllReminders(onResult: (Int, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val calendarId = getSavedCalendarId()
                // 直接从 Room 获取当前数据，避免 StateFlow 快照尚未发出的时序问题
                val activeMeds = repository.getAllActiveMedications().first()
                if (activeMeds.isEmpty()) {
                    onResult(0, null)
                    return@launch
                }
                // calendarId is nullable — null means no calendar, only alarms will be registered
                reminderManager.refreshAllReminders(activeMeds, calendarId)
                // 计算实际注册数（排除已耗尽且不需要提醒的药品）
                val actualCount = activeMeds.count { it.daysUntilDepletion() > 0 }
                onResult(actualCount, null)
            } catch (e: Exception) {
                Log.e("MediAlert", getApplication<MediAlertApplication>().getString(R.string.reset_reminders_failed, e.message ?: e.javaClass.simpleName), e)
                onResult(-1, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    /**
     * CSV 导入结果
     */
    data class ImportResult(
        val successCount: Int,
        val skipCount: Int,
        val errors: List<String>
    )

    /**
     * 导出 CSV（返回 Pair<路径, 错误信息>，成功时 error 为 null）
     */
    suspend fun exportToCsv(context: Context): Pair<String?, String?> {
        return withContext(Dispatchers.IO) {
            var fileOutputStream: FileOutputStream? = null
            var writer: CSVWriter? = null
            try {
                val medications = activeMedications.value
                if (medications.isEmpty()) {
                    Log.w("MediAlert", "CSV export skipped: no medications")
                val noDataMsg = context.getString(R.string.csv_export_no_data)
                return@withContext null to noDataMsg
                }

                val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
                    ?: context.filesDir
                val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    .format(java.time.LocalDateTime.now())
                val fileName = context.getString(R.string.csv_export_filename, timestamp)
                val file = File(dir, fileName)

                fileOutputStream = FileOutputStream(file)
                val outputStreamWriter = OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)
                // 写入 UTF-8 BOM，确保 Excel 正确识别中文
                fileOutputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                writer = CSVWriter(outputStreamWriter)

                // 表头
                writer.writeNext(arrayOf(
                    context.getString(R.string.csv_header_generic_name),
                    context.getString(R.string.csv_header_brand_name),
                    context.getString(R.string.csv_header_spec),
                    context.getString(R.string.csv_header_package),
                    context.getString(R.string.csv_header_dosage_form),
                    context.getString(R.string.csv_header_package_size),
                    context.getString(R.string.csv_header_current_stock),
                    context.getString(R.string.csv_header_frequency),
                    context.getString(R.string.csv_header_depletion_date),
                    context.getString(R.string.csv_header_status)
                ))

                // 数据
                medications.forEach { med ->
                    val freqText = when (med.frequencyType) {
                        FrequencyType.EVERY_X_DAYS -> context.getString(R.string.freq_every_x_days_format, med.frequencyValue, med.dailyDosage, med.dosageForm)
                        FrequencyType.EVERY_XTH_DAY -> context.getString(R.string.freq_every_xth_day_format, med.frequencyValue, med.dailyDosage, med.dosageForm)
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
                        if (med.isActive) context.getString(R.string.csv_status_active) else context.getString(R.string.csv_status_inactive)
                    ))
                }

                writer.flush()
                Log.i("MediAlert", "CSV exported to: ${file.absolutePath}")
                file.absolutePath to null
            } catch (e: Exception) {
                val msg = context.getString(R.string.csv_export_failed, e.message ?: e.javaClass.simpleName)
                Log.e("MediAlert", msg, e)
                null to msg
            } finally {
                try { writer?.close() } catch (_: Exception) {}
                try { fileOutputStream?.close() } catch (_: Exception) {}
            }
        }
    }

    // ──────────────────────────────────────────────
    // CSV 导入
    // ──────────────────────────────────────────────

    // 中文和英文频率解析正则（向后兼容旧CSV + 支持新英文CSV）
    private val freqRegexEveryDayZh = Regex("""每(\d+)天([\d.]+).+""")
    private val freqRegexEveryXthZh = Regex("""每隔(\d+)天([\d.]+).+""")
    private val freqRegexEveryDayEn = Regex("""(?i)Every\s+(\d+)\s*days?\s+([\d.]+).+""")
    private val freqRegexEveryXthEn = Regex("""(?i)Every\s+(\d+)th\s*day\s+([\d.]+).+""")

    /**
     * 从 CSV 文件导入药品数据
     */
    suspend fun importFromCsv(context: Context, uri: Uri): ImportResult {
        return withContext(Dispatchers.IO) {
            val successList = mutableListOf<String>()
            val errorList = mutableListOf<String>()
            var skipCount = 0

            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext ImportResult(0, 0, listOf(context.getString(R.string.csv_import_cannot_open)))

                val reader = CSVReader(BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)))
                reader.use { csv ->
                    val allRows = csv.readAll()
                    if (allRows.isEmpty()) {
                        return@withContext ImportResult(0, 0, listOf(context.getString(R.string.csv_import_empty)))
                    }
                    if (allRows.size < 2) {
                        return@withContext ImportResult(0, 0, listOf(context.getString(R.string.csv_import_no_rows)))
                    }

                    for (i in 1 until allRows.size) {
                        val row = allRows[i]
                        val rowNum = i + 1
                        try {
                            if (row.size < 8) {
                                errorList.add(context.getString(R.string.csv_import_insufficient_cols, rowNum))
                                continue
                            }

                            val genericName = row[0].trimStart('\uFEFF').trim()
                            if (genericName.isEmpty()) {
                                skipCount++
                                continue
                            }

                            val brandName = row[1].trim().takeIf { it.isNotEmpty() }
                            val specification = row[2].trim().takeIf { it.isNotEmpty() }
                            val packageUnit = row[3].trim().takeIf { it.isNotEmpty() } ?: context.getString(R.string.csv_header_package).lowercase()
                            val dosageForm = row[4].trim().takeIf { it.isNotEmpty() } ?: context.getString(R.string.csv_header_dosage_form).lowercase()

                            val packageSize = row[5].trim().toDoubleOrNull()
                            if (packageSize == null || packageSize <= 0) {
                                errorList.add(context.getString(R.string.csv_import_invalid_package_size, rowNum))
                                continue
                            }

                            val stockDisplay = row[6].trim()
                            val freqText = row[7].trim()

                            val freqParsed = parseFrequencyText(freqText)
                            if (freqParsed == null) {
                                errorList.add(context.getString(R.string.csv_import_unrecognized_freq, rowNum, freqText))
                                continue
                            }
                            val (freqType, freqValue, dailyDosage) = freqParsed

                            val currentStock = parseStockDisplay(stockDisplay, packageUnit, dosageForm, packageSize)

                            val medication = Medication(
                                genericName = genericName,
                                brandName = brandName,
                                specification = specification,
                                packageUnit = packageUnit,
                                dosageForm = dosageForm,
                                packageSize = packageSize,
                                currentStock = currentStock,
                                frequencyType = freqType,
                                frequencyValue = freqValue,
                                dailyDosage = dailyDosage,
                                startDate = null,
                                isActive = true
                            )

                            repository.addMedication(medication)
                            successList.add(genericName)
                        } catch (e: Exception) {
                            Log.w("MediAlert", "CSV import row $rowNum failed", e)
                            errorList.add(context.getString(R.string.csv_import_row_error, rowNum, e.message ?: context.getString(R.string.csv_import_unknown_error, rowNum)))
                        }
                    }
                }
                Log.i("MediAlert", "CSV import done: ${successList.size} success, $skipCount skip, ${errorList.size} errors")
                ImportResult(successList.size, skipCount, errorList)
            } catch (e: Exception) {
                Log.e("MediAlert", "CSV import failed", e)
                ImportResult(successList.size, skipCount,
                    errorList + context.getString(R.string.csv_import_file_failed, e.message ?: e.javaClass.simpleName))
            }
        }
    }

    /**
     * 解析库存显示文本（如 "2盒3片"、"0.5片"）回 Double
     */
    private fun parseStockDisplay(
        stockDisplay: String,
        packageUnit: String,
        dosageForm: String,
        packageSize: Double
    ): Double {
        val pkgIndex = stockDisplay.indexOf(packageUnit)
        val packages = if (pkgIndex >= 0) {
            stockDisplay.substring(0, pkgIndex).toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }
        val remainderStr = if (pkgIndex >= 0) {
            stockDisplay.substring(pkgIndex + packageUnit.length)
        } else {
            stockDisplay
        }
        val remainder = if (remainderStr.endsWith(dosageForm)) {
            remainderStr.removeSuffix(dosageForm).toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }
        return packages * packageSize + remainder
    }

    /**
     * 解析用药频率文本（如 "每1天1片"、"每隔2天0.5片"）
     */
    private fun parseFrequencyText(freqText: String): Triple<FrequencyType, Int, Double>? {
        // 先尝试中文正则
        freqRegexEveryDayZh.matchEntire(freqText)?.let {
            val freqValue = it.groupValues[1].toIntOrNull() ?: return null
            val dailyDosage = it.groupValues[2].toDoubleOrNull() ?: return null
            return Triple(FrequencyType.EVERY_X_DAYS, freqValue, dailyDosage)
        }
        freqRegexEveryXthZh.matchEntire(freqText)?.let {
            val freqValue = it.groupValues[1].toIntOrNull() ?: return null
            val dailyDosage = it.groupValues[2].toDoubleOrNull() ?: return null
            return Triple(FrequencyType.EVERY_XTH_DAY, freqValue, dailyDosage)
        }
        // 再尝试英文正则
        freqRegexEveryDayEn.matchEntire(freqText)?.let {
            val freqValue = it.groupValues[1].toIntOrNull() ?: return null
            val dailyDosage = it.groupValues[2].toDoubleOrNull() ?: return null
            return Triple(FrequencyType.EVERY_X_DAYS, freqValue, dailyDosage)
        }
        freqRegexEveryXthEn.matchEntire(freqText)?.let {
            val freqValue = it.groupValues[1].toIntOrNull() ?: return null
            val dailyDosage = it.groupValues[2].toDoubleOrNull() ?: return null
            return Triple(FrequencyType.EVERY_XTH_DAY, freqValue, dailyDosage)
        }
        return null
    }
}
