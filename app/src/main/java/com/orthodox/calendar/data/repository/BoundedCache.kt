package com.orthodox.calendar.data.repository

/**
 * A map that forgets its least recently used entries past [maxEntries].
 *
 * The repository used an unbounded `ConcurrentHashMap` for decoded years: one
 * resolved year is 365 days of feasts, readings and biography text, all of it
 * referencing the locale's text pool, so browsing 2024-2099 across four locales
 * retained every year it had ever touched and `largeHeap` absorbed the
 * difference. Six keys covers months of tapping through one locale plus the
 * neighbour year the fasting-season spans need, and evicting the rest costs a
 * re-read of a file that is on disk (or in assets) anyway.
 *
 * [LinkedHashMap] with `accessOrder = true` gives the LRU order — every `get`
 * touches that order, so the whole map is guarded by one monitor instead of
 * pretending to be a `ConcurrentHashMap`. The critical sections are plain map
 * operations: no I/O, and nothing suspended while the lock is held.
 */
internal class BoundedCache<K, V>(private val maxEntries: Int) {

    private val map = LinkedHashMap<K, V>(16, 0.75f, true)

    init {
        require(maxEntries > 0) { "maxEntries must be positive" }
    }

    @Synchronized
    operator fun get(key: K): V? = map[key]

    @Synchronized
    operator fun set(key: K, value: V) {
        map[key] = value
        evictEldest()
    }

    @Synchronized
    fun clear() = map.clear()

    /** Snapshot of the keys, least recently used first. For tests. */
    @Synchronized
    fun keys(): List<K> = map.keys.toList()

    val size: Int
        @Synchronized get() = map.size

    private fun evictEldest() {
        while (map.size > maxEntries) {
            val it = map.entries.iterator()
            if (!it.hasNext()) break
            it.next()
            it.remove()
        }
    }
}
