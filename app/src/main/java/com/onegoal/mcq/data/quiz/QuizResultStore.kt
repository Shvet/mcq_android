package com.onegoal.mcq.data.quiz

import com.onegoal.mcq.model.QuizResult
import java.util.concurrent.atomic.AtomicLong

/**
 * Hands a finished test from the test screen to the result screen without pushing
 * every question through a navigation argument. Keeps only the latest few results in memory.
 */
class QuizResultStore {
    private val nextId = AtomicLong(1)
    private val results = LinkedHashMap<Long, QuizResult>()

    @Synchronized
    fun put(result: QuizResult): Long {
        val id = nextId.getAndIncrement()
        results[id] = result
        while (results.size > MAX_RESULTS) results.remove(results.keys.first())
        return id
    }

    @Synchronized
    fun get(id: Long): QuizResult? = results[id]

    private companion object {
        const val MAX_RESULTS = 5
    }
}
