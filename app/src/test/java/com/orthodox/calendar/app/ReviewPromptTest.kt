package com.orthodox.calendar.app

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * The rating ask counts separate days of use, not launches, and fires at most
 * once per app version.
 *
 * Mirror of `OrthodoxCalendarTests/ReviewPromptTests.swift`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReviewPromptTest {
    private val prefs = RuntimeEnvironment.getApplication()
        .getSharedPreferences("review_prompt_test", Context.MODE_PRIVATE)

    private fun day(n: Long): LocalDate = LocalDate.of(2026, 9, 1).plusDays(n)

    @Before
    fun clear() {
        prefs.edit().clear().commit()
    }

    @Test
    fun `same day counts once`() {
        val prompt = ReviewPrompt(prefs, "1.5.1")
        repeat(10) { prompt.recordActive(day(0)) }
        assertEquals(1, prompt.activeDays)
        assertFalse(prompt.shouldPrompt)
    }

    @Test
    fun `asks after required days`() {
        val prompt = ReviewPrompt(prefs, "1.5.1")
        for (n in 0 until ReviewPrompt.REQUIRED_DAYS - 1) prompt.recordActive(day(n.toLong()))
        assertFalse(prompt.shouldPrompt)
        prompt.recordActive(day(ReviewPrompt.REQUIRED_DAYS.toLong()))
        assertTrue(prompt.shouldPrompt)
    }

    @Test
    fun `once per version`() {
        val prompt = ReviewPrompt(prefs, "1.5.1")
        for (n in 0 until ReviewPrompt.REQUIRED_DAYS) prompt.recordActive(day(n.toLong()))
        prompt.markPrompted()
        for (n in 10L until 30L) prompt.recordActive(day(n))
        assertFalse(prompt.shouldPrompt)

        // A new version starts from the days used since the last ask.
        assertTrue(ReviewPrompt(prefs, "1.6.0").shouldPrompt)
    }
}
