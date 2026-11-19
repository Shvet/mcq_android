package com.onegoal.mcq.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onegoal.mcq.data.McqRepository
import com.onegoal.mcq.model.HomeSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.onegoal.mcq.data.fallbackHomeSections

sealed interface HomeSectionsUiState {
    data object Loading : HomeSectionsUiState
    data class Loaded(val sections: List<HomeSection>) : HomeSectionsUiState
    data object Error : HomeSectionsUiState
}

/**
 * Loads the home screen: the sections built in admin for this app, or (when none are set up)
 * a built-in home of its PDF and MCQ categories, so the screen is never empty.
 */
class HomeSectionsViewModel(
    private val repository: McqRepository,
    private val pdfTitle: String,
    private val quizTitle: String,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeSectionsUiState>(HomeSectionsUiState.Loading)
    val state: StateFlow<HomeSectionsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = HomeSectionsUiState.Loading
        viewModelScope.launch {
            _state.value = runCatching {
                repository.homeSections().ifEmpty {
                    coroutineScope {
                        val pdfs = async { runCatching { repository.homeItems() }.getOrDefault(emptyList()) }
                        val quizzes = async { runCatching { repository.quizHome() }.getOrDefault(emptyList()) }
                        fallbackHomeSections(pdfTitle, pdfs.await(), quizTitle, quizzes.await())
                    }
                }
            }.fold(
                onSuccess = { HomeSectionsUiState.Loaded(it) },
                onFailure = { HomeSectionsUiState.Error },
            )
        }
    }
}
