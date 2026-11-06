package com.onegoal.mcq

import com.onegoal.mcq.model.AnswerStatus
import com.onegoal.mcq.model.NodeLevel
import com.onegoal.mcq.model.QuizNode
import com.onegoal.mcq.model.QuizOption
import com.onegoal.mcq.model.QuizQuestion
import com.onegoal.mcq.model.QuizResult
import com.onegoal.mcq.model.formatDuration
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class QuizResultTest {

    private val test = QuizNode(id = 3, level = NodeLevel.SUB_CATEGORY, title = "Atoms", questionCount = 4)

    private fun question(id: Long, correct: Int, optionCount: Int = 4) = QuizQuestion(
        id = id,
        text = "Question $id",
        options = (1..optionCount).map { QuizOption(number = it, label = ('A' + it - 1).toString(), text = "Option $it") },
        correctOption = correct,
    )

    private val questions = listOf(question(1, 1), question(2, 2), question(3, 3), question(4, 5, optionCount = 5))

    @Test
    fun countsRightWrongAndUnattempted() {
        val result = QuizResult.of(test, questions, answers = mapOf(1L to 1, 2L to 4, 4L to 5), elapsedMillis = 90_000)

        assertEquals(4, result.total)
        assertEquals(2, result.correct)
        assertEquals(1, result.wrong)
        assertEquals(1, result.unattempted)
        assertEquals(3, result.attempted)
        assertEquals(0.5f, result.score, 0.0001f)
        assertEquals(2f / 3f, result.accuracy, 0.0001f)
        assertEquals(30_000L, result.millisPerQuestion)
        assertEquals(AnswerStatus.UNATTEMPTED, result.questions[2].status)
        assertEquals(AnswerStatus.WRONG, result.questions[1].status)
    }

    @Test
    fun answersThatAreNotAnOptionCountAsUnattempted() {
        val result = QuizResult.of(test, questions, answers = mapOf(1L to 5), elapsedMillis = 0)

        assertEquals(4, result.unattempted)
        assertEquals(0f, result.accuracy, 0f)
        assertEquals(0L, result.millisPerQuestion)
    }

    @Test
    fun formatsDurations() {
        assertEquals("00:00", formatDuration(0))
        assertEquals("01:05", formatDuration(65_400))
        assertEquals("1:02:09", formatDuration(3_729_000))
    }

    @Test
    fun quizNodeSurvivesNavigationArgumentRoundTrip() {
        val json = Json { ignoreUnknownKeys = true }
        val encoded = json.encodeToString(QuizNode.serializer(), test)
        assertEquals(test, json.decodeFromString(QuizNode.serializer(), encoded))
    }
}
