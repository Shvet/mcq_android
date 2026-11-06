package com.onegoal.mcq.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onegoal.mcq.data.McqRepository
import com.onegoal.mcq.model.QuizNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface QuizListUiState {
    data object Loading : QuizListUiState
    data class Loaded(val items: List<QuizNode>) : QuizListUiState
    data object Error : QuizListUiState
}

/** Loads the MCQ tab's top level (parent == null) or the children of an MCQ folder. */
class QuizListViewModel(
    private val repository: McqRepository,
    private val parent: QuizNode?,
) : ViewModel() {

    private val _state = MutableStateFlow<QuizListUiState>(QuizListUiState.Loading)
    val state: StateFlow<QuizListUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = QuizListUiState.Loading
        viewModelScope.launch {
            _state.value = runCatching {
                if (parent == null) repository.quizHome() else repository.quizChildren(parent)
            }.fold(
                onSuccess = { QuizListUiState.Loaded(it) },
                onFailure = { QuizListUiState.Error },
            )
        }
    }
}
