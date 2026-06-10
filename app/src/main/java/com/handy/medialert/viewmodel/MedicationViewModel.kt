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
            reminderManager.refreshAllReminders(activeMedications.value, calendarId)
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
                val activeMeds = activeMedications.value.filter { it.isActive }
                if (activeMeds.isEmpty()) {
                    onResult(0, null)
                    return@launch
                }
                // calendarId is nullable — null means no calendar, only alarms will be registered
                reminderManager.refreshAllReminders(activeMeds, calendarId)
                onResult(activeMeds.size, null)
            } catch (e: Exception) {
                Log.e("MediAlert", "重设提醒失败", e)
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
                    return@withContext null to "没有药品数据可导出"
                }

                val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
                    ?: context.filesDir
                val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    .format(java.time.LocalDateTime.now())
                val fileName = "药箱库存_$timestamp.csv"
                val file = File(dir, fileName)

                fileOutputStream = FileOutputStream(file)
                val outputStreamWriter = OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)
                // 写入 UTF-8 BOM，确保 Excel 正确识别中文
                fileOutputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                writer = CSVWriter(outputStreamWriter)

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

                writer.flush()
                Log.i("MediAlert", "CSV exported to: ${file.absolutePath}")
                file.absolutePath to null
            } catch (e: Exception) {
                val msg = "导出失败: ${e.message ?: e.javaClass.simpleName}"
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

    private val freqRegexEveryDay = Regex("""每(\d+)天([\d.]+).+""")
    private val freqRegexEveryXth = Regex("""每隔(\d+)天([\d.]+).+""")

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
                    ?: return@withContext ImportResult(0, 0, listOf("无法打开文件"))

                val reader = CSVReader(BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)))
                reader.use { csv ->
                    val allRows = csv.readAll()
                    if (allRows.isEmpty()) {
                        return@withContext ImportResult(0, 0, listOf("文件为空"))
                    }
                    if (allRows.size < 2) {
                        return@withContext ImportResult(0, 0, listOf("文件中没有数据行"))
                    }

                    for (i in 1 until allRows.size) {
                        val row = allRows[i]
                        val rowNum = i + 1
                        try {
                            if (row.size < 8) {
                                errorList.add("第${rowNum}行: 列数不足（需要至少8列）")
                                continue
                            }

                            val genericName = row[0].trimStart('\uFEFF').trim()
                            if (genericName.isEmpty()) {
                                skipCount++
                                continue
                            }

                            val brandName = row[1].trim().takeIf { it.isNotEmpty() }
                            val specification = row[2].trim().takeIf { it.isNotEmpty() }
                            val packageUnit = row[3].trim().takeIf { it.isNotEmpty() } ?: "盒"
                            val dosageForm = row[4].trim().takeIf { it.isNotEmpty() } ?: "片"

                            val packageSize = row[5].trim().toIntOrNull()
                            if (packageSize == null || packageSize <= 0) {
                                errorList.add("第${rowNum}行: 每包装数量无效")
                                continue
                            }

                            val stockDisplay = row[6].trim()
                            val freqText = row[7].trim()

                            val freqParsed = parseFrequencyText(freqText)
                            if (freqParsed == null) {
                                errorList.add("第${rowNum}行: 用药频率格式无法识别 '$freqText'")
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
                            errorList.add("第${rowNum}行: ${e.message ?: "未知错误"}")
                        }
                    }
                }
                Log.i("MediAlert", "CSV import done: ${successList.size} success, $skipCount skip, ${errorList.size} errors")
                ImportResult(successList.size, skipCount, errorList)
            } catch (e: Exception) {
                Log.e("MediAlert", "CSV import failed", e)
                ImportResult(successList.size, skipCount,
                    errorList + "文件读取失败: ${e.message ?: e.javaClass.simpleName}")
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
        packageSize: Int
    ): Double {
        val pkgIndex = stockDisplay.indexOf(packageUnit)
        val packages = if (pkgIndex >= 0) {
            stockDisplay.substring(0, pkgIndex).toIntOrNull() ?: 0
        } else {
            0
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
        freqRegexEveryDay.matchEntire(freqText)?.let {
            val freqValue = it.groupValues[1].toIntOrNull() ?: return null
            val dailyDosage = it.groupValues[2].toDoubleOrNull() ?: return null
            return Triple(FrequencyType.EVERY_X_DAYS, freqValue, dailyDosage)
        }
        freqRegexEveryXth.matchEntire(freqText)?.let {
            val freqValue = it.groupValues[1].toIntOrNull() ?: return null
            val dailyDosage = it.groupValues[2].toDoubleOrNull() ?: return null
            return Triple(FrequencyType.EVERY_XTH_DAY, freqValue, dailyDosage)
        }
        return null
    }
}
