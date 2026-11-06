package com.onegoal.mcq

import com.onegoal.mcq.data.quiz.QuizResultStore
import com.onegoal.mcq.model.NodeLevel
import com.onegoal.mcq.model.QuizNode
import com.onegoal.mcq.model.QuizResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuizResultStoreTest {

    private fun result(title: String) =
        QuizResult(QuizNode(id = 1, level = NodeLevel.CATEGORY, title = title), emptyList(), elapsedMillis = 0)

    @Test
    fun keepsOnlyTheLatestResults() {
        val store = QuizResultStore()
        val ids = (1..7).map { store.put(result("T$it")) }

        assertNull(store.get(ids[0]))
        assertNull(store.get(ids[1]))
        assertEquals("T7", store.get(ids[6])?.test?.title)
        assertEquals("T3", store.get(ids[2])?.test?.title)
    }
}
