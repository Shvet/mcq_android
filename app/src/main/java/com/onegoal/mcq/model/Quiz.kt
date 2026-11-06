package com.onegoal.mcq.model

import kotlinx.serialization.Serializable

/** A row of the MCQ tree: a folder with children, or a test (a leaf) with questions. */
@Serializable
data class QuizNode(
    val id: Long,
    val level: NodeLevel,
    val title: String,
    val parentTitle: String? = null,
    val imageUrl: String? = null,
    val hasChildren: Boolean = false,
    val questionCount: Int = 0,
) {
    val isTest: Boolean get() = !hasChildren
    val key: String get() = "${level.name}:$id"
}

/** One answer of a question. Text, image or both. */
@Serializable
data class QuizOption(
    val number: Int,
    val label: String,
    val text: String? = null,
    val imageUrl: String? = null,
)

@Serializable
data class QuizQuestion(
    val id: Long,
    val text: String? = null,
    val imageUrl: String? = null,
    val options: List<QuizOption>,
    /** [QuizOption.number] of the right answer. */
    val correctOption: Int,
    /** Admin's explanation for the right answer. */
    val note: String? = null,
)

enum class AnswerStatus { CORRECT, WRONG, UNATTEMPTED }

data class QuestionResult(
    val index: Int,
    val question: QuizQuestion,
    val selectedOption: Int?,
) {
    val status: AnswerStatus
        get() = when (selectedOption) {
            null -> AnswerStatus.UNATTEMPTED
            question.correctOption -> AnswerStatus.CORRECT
            else -> AnswerStatus.WRONG
        }
}

/** Outcome of one finished test session. */
data class QuizResult(
    val test: QuizNode,
    val questions: List<QuestionResult>,
    val elapsedMillis: Long,
) {
    val total: Int get() = questions.size
    val correct: Int get() = questions.count { it.status == AnswerStatus.CORRECT }
    val wrong: Int get() = questions.count { it.status == AnswerStatus.WRONG }
    val unattempted: Int get() = questions.count { it.status == AnswerStatus.UNATTEMPTED }
    val attempted: Int get() = total - unattempted

    /** Share of all questions answered right, 0..1. */
    val score: Float get() = if (total == 0) 0f else correct.toFloat() / total

    /** Share of attempted questions answered right, 0..1. */
    val accuracy: Float get() = if (attempted == 0) 0f else correct.toFloat() / attempted

    val millisPerQuestion: Long get() = if (attempted == 0) 0 else elapsedMillis / attempted

    companion object {
        fun of(test: QuizNode, questions: List<QuizQuestion>, answers: Map<Long, Int>, elapsedMillis: Long) = QuizResult(
            test = test,
            questions = questions.mapIndexed { index, question ->
                // ignore answers that do not match an option (e.g. stale state)
                val selected = answers[question.id]?.takeIf { number -> question.options.any { it.number == number } }
                QuestionResult(index, question, selected)
            },
            elapsedMillis = elapsedMillis,
        )
    }
}

/** "4:05" or "1:02:09". */
fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
