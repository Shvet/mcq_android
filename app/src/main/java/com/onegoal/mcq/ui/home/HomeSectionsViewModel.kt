package com.onegoal.mcq.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onegoal.mcq.data.McqRepository
import com.onegoal.mcq.model.HomeSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeSectionsUiState {
    data object Loading : HomeSectionsUiState
    data class Loaded(val sections: List<HomeSection>) : HomeSectionsUiState
    data object Error : HomeSectionsUiState
}

/** Loads the Home tab: the sections built in admin for this app. */
class HomeSectionsViewModel(private val repository: McqRepository) : ViewModel() {

    private val _state = MutableStateFlow<HomeSectionsUiState>(HomeSectionsUiState.Loading)
    val state: StateFlow<HomeSectionsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = HomeSectionsUiState.Loading
        viewModelScope.launch {
            _state.value = runCatching { repository.homeSections() }.fold(
                onSuccess = { HomeSectionsUiState.Loaded(it) },
                onFailure = { HomeSectionsUiState.Error },
            )
        }
    }
}
