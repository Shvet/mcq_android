package com.onegoal.mcq.ui.quiz

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onegoal.mcq.data.McqRepository
import com.onegoal.mcq.data.quiz.QuizResultStore
import com.onegoal.mcq.model.QuizNode
import com.onegoal.mcq.model.QuizQuestion
import com.onegoal.mcq.model.QuizResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QuizPhase { Loading, Error, Empty, Intro, Running }

data class QuizTestUiState(
    val phase: QuizPhase = QuizPhase.Loading,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    /** question id -> chosen option number */
    val answers: Map<Long, Int> = emptyMap(),
    val paused: Boolean = false,
) {
    val current: QuizQuestion? get() = questions.getOrNull(currentIndex)
    val answeredCount: Int get() = answers.size
    val isLast: Boolean get() = currentIndex == questions.lastIndex
}

/**
 * One test session. The timer starts only when the user taps Start, can be paused,
 * and stops when the test is finished.
 */
class QuizTestViewModel(
    private val repository: McqRepository,
    private val resultStore: QuizResultStore,
    val test: QuizNode,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {

    private val _state = MutableStateFlow(QuizTestUiState())
    val state: StateFlow<QuizTestUiState> = _state.asStateFlow()

    /** Time banked before the last resume. */
    private var accumulatedMillis = 0L

    /** Clock value of the last start/resume, or null while not running. */
    private var runningSince: Long? = null

    init {
        load()
    }

    fun load() {
        _state.value = QuizTestUiState(phase = QuizPhase.Loading)
        viewModelScope.launch {
            _state.value = runCatching { repository.quizQuestions(test) }.fold(
                onSuccess = { questions ->
                    QuizTestUiState(phase = if (questions.isEmpty()) QuizPhase.Empty else QuizPhase.Intro, questions = questions)
                },
                onFailure = { QuizTestUiState(phase = QuizPhase.Error) },
            )
        }
    }

    fun start() {
        if (_state.value.phase != QuizPhase.Intro) return
        accumulatedMillis = 0
        runningSince = clock()
        _state.update { it.copy(phase = QuizPhase.Running, currentIndex = 0, answers = emptyMap(), paused = false) }
    }

    fun elapsedMillis(): Long = accumulatedMillis + (runningSince?.let { clock() - it } ?: 0L)

    fun pause() {
        val since = runningSince ?: return
        accumulatedMillis += clock() - since
        runningSince = null
        _state.update { it.copy(paused = true) }
    }

    fun resume() {
        if (runningSince != null || _state.value.phase != QuizPhase.Running) return
        runningSince = clock()
        _state.update { it.copy(paused = false) }
    }

    /** Selecting the already chosen answer clears it, so a question can be left unattempted again. */
    fun select(optionNumber: Int) {
        val question = _state.value.current ?: return
        _state.update { state ->
            val answers = if (state.answers[question.id] == optionNumber) {
                state.answers - question.id
            } else {
                state.answers + (question.id to optionNumber)
            }
            state.copy(answers = answers)
        }
    }

    fun goTo(index: Int) {
        _state.update { it.copy(currentIndex = index.coerceIn(0, (it.questions.size - 1).coerceAtLeast(0))) }
    }

    fun next() = goTo(_state.value.currentIndex + 1)

    fun previous() = goTo(_state.value.currentIndex - 1)

    /** Stops the timer and returns the id of the stored result for the result screen. */
    fun finish(): Long {
        val elapsed = elapsedMillis()
        runningSince = null
        accumulatedMillis = elapsed
        val state = _state.value
        return resultStore.put(QuizResult.of(test, state.questions, state.answers, elapsed))
    }
}
