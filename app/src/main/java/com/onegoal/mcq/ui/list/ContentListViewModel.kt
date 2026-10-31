package com.onegoal.mcq.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onegoal.mcq.data.McqRepository
import com.onegoal.mcq.model.ContentItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ListUiState {
    data object Loading : ListUiState
    data class Loaded(val items: List<ContentItem>) : ListUiState
    data object Error : ListUiState
}

/** Loads the home list (parent == null) or the children of a folder row. */
class ContentListViewModel(
    private val repository: McqRepository,
    private val parent: ContentItem?,
) : ViewModel() {

    private val _state = MutableStateFlow<ListUiState>(ListUiState.Loading)
    val state: StateFlow<ListUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = ListUiState.Loading
        viewModelScope.launch {
            _state.value = runCatching {
                if (parent == null) repository.homeItems() else repository.children(parent)
            }.fold(
                onSuccess = { ListUiState.Loaded(it) },
                onFailure = { ListUiState.Error },
            )
        }
    }
}
