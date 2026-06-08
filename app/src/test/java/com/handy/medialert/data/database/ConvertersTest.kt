package com.handy.medialert.data.database

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    // ============================================================
    // fromLocalDate() 日期 → 字符串
    // ============================================================

    @Test
    fun fromLocalDate_validDate_returnsIsoString() {
        val date = LocalDate.of(2026, 6, 8)
        assertEquals("2026-06-08", converters.fromLocalDate(date))
    }

    @Test
    fun fromLocalDate_null_returnsNull() {
        assertNull(converters.fromLocalDate(null))
    }

    @Test
    fun fromLocalDate_leapYear_returnsCorrectly() {
        val date = LocalDate.of(2024, 2, 29)
        assertEquals("2024-02-29", converters.fromLocalDate(date))
    }

    // ============================================================
    // toLocalDate() 字符串 → 日期
    // ============================================================

    @Test
    fun toLocalDate_validString_returnsLocalDate() {
        val date = converters.toLocalDate("2026-06-08")
        assertEquals(LocalDate.of(2026, 6, 8), date)
    }

    @Test
    fun toLocalDate_null_returnsNull() {
        assertNull(converters.toLocalDate(null))
    }

    @Test
    fun toLocalDate_leapYear_returnsCorrectly() {
        val date = converters.toLocalDate("2024-02-29")
        assertEquals(LocalDate.of(2024, 2, 29), date)
    }

    // ============================================================
    // 双向转换一致性 (round-trip)
    // ============================================================

    @Test
    fun roundTrip_localDateToStringToLocalDate_preservesValue() {
        val original = LocalDate.of(2026, 12, 31)
        val result = converters.toLocalDate(converters.fromLocalDate(original))
        assertEquals(original, result)
    }

    @Test
    fun roundTrip_stringToLocalDateToString_preservesValue() {
        val original = "2026-01-15"
        val result = converters.fromLocalDate(converters.toLocalDate(original))
        assertEquals(original, result)
    }

    @Test
    fun roundTrip_nullToNull_preservesNull() {
        assertNull(converters.toLocalDate(converters.fromLocalDate(null)))
    }
}
