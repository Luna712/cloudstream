package com.lagradost.cloudstream3

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class IntToYearTest {

    @Test
    fun toYearReturnsFirstOfJanuary() {
        val result = 2026.toYear()
        assertEquals(LocalDate(2026, 1, 1), result)
    }

    @Test
    fun toYearSetsCorrectYear() {
        val result = 1999.toYear()
        assertEquals(1999, result.year)
    }

    @Test
    fun toYearSetsMonthToJanuary() {
        val result = 2026.toYear()
        assertEquals(1, result.month)
    }

    @Test
    fun toYearSetsDayToFirst() {
        val result = 2026.toYear()
        assertEquals(1, result.day)
    }

    @Test
    fun toYearHandlesLeapYear() {
        val result = 2028.toYear()
        assertEquals(LocalDate(2028, 1, 1), result)
    }

    @Test
    fun toYearHandlesEpochYear() {
        val result = 1970.toYear()
        assertEquals(LocalDate(1970, 1, 1), result)
    }

    @Test
    fun toYearHandlesFarPastYear() {
        val result = 1.toYear()
        assertEquals(LocalDate(1, 1, 1), result)
    }

    @Test
    fun toYearHandlesFarFutureYear() {
        val result = 9999.toYear()
        assertEquals(LocalDate(9999, 1, 1), result)
    }

    @Test
    fun toYearDifferentYearsProduceDifferentDates() {
        assertFalse(2025.toYear() == 2026.toYear())
    }
}
