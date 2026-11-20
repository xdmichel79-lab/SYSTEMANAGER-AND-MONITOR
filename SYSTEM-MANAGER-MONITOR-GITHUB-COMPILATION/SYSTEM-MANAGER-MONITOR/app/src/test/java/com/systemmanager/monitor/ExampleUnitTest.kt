package com.systemmanager.monitor

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun ramPercentageCalculation_isCorrect() {
        val total = 8_000L
        val used = 2_000L
        val percentage = (used * 100 / total).toInt()
        assertEquals(25, percentage)
    }
}
