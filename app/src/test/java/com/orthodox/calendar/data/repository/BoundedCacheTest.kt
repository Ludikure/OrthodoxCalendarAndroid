package com.orthodox.calendar.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The decoded-year cache's bound.
 *
 * The repository used an unbounded map, so every year a user ever tapped stayed
 * reachable — 2024 to 2099 across four locales — and `largeHeap` absorbed the
 * difference. These tests pin the eviction the repository now depends on.
 */
class BoundedCacheTest {

    @Test
    fun `stores and reads back`() {
        val cache = BoundedCache<String, String>(maxEntries = 3)
        cache["a"] = "one"
        assertEquals("one", cache["a"])
        assertEquals(1, cache.size)
    }

    @Test
    fun `evicts the least recently used once past the bound`() {
        val cache = BoundedCache<String, Int>(maxEntries = 3)
        cache["a"] = 1
        cache["b"] = 2
        cache["c"] = 3
        // "a" is the oldest untouched entry and goes.
        cache["d"] = 4
        assertNull(cache["a"])
        assertEquals(3, cache.size)
        assertEquals(listOf("b", "c", "d"), cache.keys())
    }

    /**
     * The point of `accessOrder = true`: reading counts as use. Without it the
     * cache would evict the year currently on screen while a user pages through
     * months, which is the one entry that must survive.
     */
    @Test
    fun `a read protects an entry from eviction`() {
        val cache = BoundedCache<String, Int>(maxEntries = 3)
        cache["a"] = 1
        cache["b"] = 2
        cache["c"] = 3
        assertEquals(1, cache["a"])          // touch "a"
        cache["d"] = 4                        // "b" is now the eldest
        assertNull(cache["b"])
        assertEquals(1, cache["a"])
    }

    @Test
    fun `overwriting a key does not grow the cache`() {
        val cache = BoundedCache<String, Int>(maxEntries = 2)
        cache["a"] = 1
        cache["a"] = 2
        cache["b"] = 3
        assertEquals(2, cache.size)
        assertEquals(2, cache["a"])
    }

    @Test
    fun `clear drops everything`() {
        val cache = BoundedCache<String, Int>(maxEntries = 2)
        cache["a"] = 1
        cache.clear()
        assertEquals(0, cache.size)
        assertNull(cache["a"])
    }

    @Test
    fun `a single slot cache keeps exactly one entry`() {
        val cache = BoundedCache<String, Int>(maxEntries = 1)
        cache["a"] = 1
        cache["b"] = 2
        assertNull(cache["a"])
        assertEquals(2, cache["b"])
    }
}
