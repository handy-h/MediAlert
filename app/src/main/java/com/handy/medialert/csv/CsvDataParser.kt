package com.handy.medialert.csv

import com.handy.medialert.data.entity.FrequencyType

/**
 * CSV 数据解析工具
 * 从 CSV 导入的频率文本 / 库存显示文本解析为结构化数据。
 * 纯函数、无 Android 依赖，便于单元测试。
 */
object CsvDataParser {

    // 中文和英文频率解析正则（向后兼容旧CSV + 支持新英文CSV）
    private val freqRegexEveryDayZh = Regex("""每(\d+)天([\d.]+).+""")
    private val freqRegexEveryXthZh = Regex("""每隔(\d+)天([\d.]+).+""")
    private val freqRegexEveryDayEn = Regex("""(?i)Every\s+(\d+)\s*days?\s+([\d.]+).+""")
    private val freqRegexEveryXthEn = Regex("""(?i)Every\s+(\d+)th\s*day\s+([\d.]+).+""")

    /**
     * 解析用药频率文本（如 "每1天1片"、"每隔2天0.5片"、"Every 1 days 1片"）
     * @return Triple(频率类型, 频率值, 每日剂量)，无法识别时返回 null
     */
    fun parseFrequencyText(freqText: String): Triple<FrequencyType, Int, Double>? {
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

    /**
     * 解析库存显示文本（如 "2盒3片"、"0.5片"）回 Double 总剂量单位
     */
    fun parseStockDisplay(
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
}
