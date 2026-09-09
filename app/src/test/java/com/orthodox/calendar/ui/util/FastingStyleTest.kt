package com.orthodox.calendar.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The seven values `fasting_engine.py` actually emits, and nothing else.
 *
 * The grid used to paint hotNoOil days as oil days because `contains("oil")`
 * was tested before the water case — true for "hotNoOil" — while the dot on the
 * same cell got it right. Four hand-written copies of this mapping made that
 * easy to miss; one function with this table makes it impossible.
 */
class FastingStyleTest {

    @Test
    fun `every fasting type the pipeline emits maps to the right bucket`() {
        val expected = mapOf(
            "totalAbstinence" to FastingStyle.STRICT,
            "dryEating" to FastingStyle.STRICT,
            "hotNoOil" to FastingStyle.WATER,
            "hotWithOil" to FastingStyle.OIL,
            "fish" to FastingStyle.FISH,
            "fishRoe" to FastingStyle.FISH,
            "free" to FastingStyle.FREE
        )
        for ((type, style) in expected) {
            assertEquals("fasting type $type", style, fastingStyle(type))
        }
    }

    @Test
    fun `hotNoOil is water even though its name contains oil`() {
        assertEquals(FastingStyle.WATER, fastingStyle("hotNoOil"))
        assertEquals(FastingStyle.OIL, fastingStyle("hotWithOil"))
    }

    @Test
    fun `an unknown type is treated as a non-fasting day rather than crashing`() {
        assertEquals(FastingStyle.FREE, fastingStyle(""))
        assertEquals(FastingStyle.FREE, fastingStyle("somethingNew"))
    }
}
