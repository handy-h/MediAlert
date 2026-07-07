package com.handy.medialert.csv

import com.handy.medialert.data.entity.FrequencyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CsvDataParserTest {

    @Test
    fun `parseFrequencyText 中文 每X天`() {
        val result = CsvDataParser.parseFrequencyText("每1天1片")
        assertEquals(Triple(FrequencyType.EVERY_X_DAYS, 1, 1.0), result)
    }

    @Test
    fun `parseFrequencyText 中文 每隔X天`() {
        val result = CsvDataParser.parseFrequencyText("每隔2天0.5片")
        assertEquals(Triple(FrequencyType.EVERY_XTH_DAY, 2, 0.5), result)
    }

    @Test
    fun `parseFrequencyText 英文 every X days`() {
        val result = CsvDataParser.parseFrequencyText("Every 1 days 1片")
        assertEquals(Triple(FrequencyType.EVERY_X_DAYS, 1, 1.0), result)
    }

    @Test
    fun `parseFrequencyText 英文 every Xth day`() {
        val result = CsvDataParser.parseFrequencyText("Every 3th day 2粒")
        assertEquals(Triple(FrequencyType.EVERY_XTH_DAY, 3, 2.0), result)
    }

    @Test
    fun `parseFrequencyText 无法识别返回 null`() {
        assertNull(CsvDataParser.parseFrequencyText("乱码文本"))
        assertNull(CsvDataParser.parseFrequencyText(""))
    }

    @Test
    fun `parseStockDisplay 盒+片`() {
        // 2盒3片, 每盒14 -> 2*14 + 3 = 31
        assertEquals(31.0, CsvDataParser.parseStockDisplay("2盒3片", "盒", "片", 14.0), 0.0001)
    }

    @Test
    fun `parseStockDisplay 仅零散`() {
        // 0.5片, 无整盒
        assertEquals(0.5, CsvDataParser.parseStockDisplay("0.5片", "盒", "片", 14.0), 0.0001)
    }

    @Test
    fun `parseStockDisplay 仅整包装`() {
        // 5瓶, 每瓶10ml -> 50
        assertEquals(50.0, CsvDataParser.parseStockDisplay("5瓶", "瓶", "ml", 10.0), 0.0001)
    }

    @Test
    fun `parseStockDisplay 单位不匹配时忽略零散部分`() {
        // 文本不含包装单位，整盒部分为0；零散部分不以剂型结尾则忽略 -> 0
        assertEquals(0.0, CsvDataParser.parseStockDisplay("abc", "盒", "片", 14.0), 0.0001)
    }
}
