package com.orthodox.calendar.data.preferences

import com.orthodox.calendar.data.model.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.IOException

/**
 * Reading a preference that might not be readable.
 *
 * `DataStore.data` throws when the preferences file is corrupt, locked, or the
 * disk is full. The ViewModel read those flows with a bare `first()`, so one
 * unreadable file killed the startup coroutine before it set a localization
 * bundle — and every screen stops while the bundle is null, which is a blank app
 * with no retry and no way back. A default is the right answer to an unreadable
 * file: it is what a first launch gets anyway.
 */
class GuardedReadTest {

    private fun <T> failing(reason: Throwable): Flow<T> = flow { throw reason }

    @Test
    fun `a corrupt preferences file yields the default`() = runTest {
        assertEquals(AppLanguage.SR, failing<AppLanguage>(IOException("corrupt")).firstOrDefault(AppLanguage.SR))
    }

    @Test
    fun `a readable preferences file yields its value`() = runTest {
        assertEquals(AppLanguage.EN, flowOf(AppLanguage.EN).firstOrDefault(AppLanguage.SR))
    }

    /**
     * A cancelled scope is not a failed read. Swallowing it would let a
     * ViewModel's coroutine run on after the thing that owned it was destroyed —
     * which is worse than the blank screen this file exists to prevent, because
     * it writes state into a dead scope.
     */
    @Test
    fun `cancellation is rethrown, not defaulted`() = runTest {
        val thrown = try {
            failing<AppLanguage>(CancellationException("scope cancelled")).firstOrDefault(AppLanguage.RU)
            null
        } catch (c: CancellationException) {
            c
        }
        assertNotNull("cancellation was swallowed and turned into a default", thrown)
    }

    /** The failure the field actually reports is an IOException from disk. */
    @Test
    fun `any other read failure also yields the default`() = runTest {
        assertEquals(AppLanguage.RU, failing<AppLanguage>(IllegalStateException("disk full")).firstOrDefault(AppLanguage.RU))
    }
}
