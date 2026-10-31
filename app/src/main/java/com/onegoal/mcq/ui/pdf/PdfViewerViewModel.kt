package com.onegoal.mcq.ui.pdf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onegoal.mcq.data.McqRepository
import com.onegoal.mcq.data.pdf.PdfFileCache
import com.onegoal.mcq.data.prefs.AppPreferences
import com.onegoal.mcq.model.ContentItem
import com.onegoal.mcq.model.ReadingPosition
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DocumentState {
    data object Loading : DocumentState
    data class Ready(val renderer: PdfDocumentRenderer) : DocumentState
    data object Error : DocumentState
}

sealed interface SearchState {
    data object Idle : SearchState
    data object Searching : SearchState
    data class Results(val query: String, val pages: List<Int>) : SearchState
}

data class ViewerPosition(val sectionIndex: Int, val page: Int)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PdfViewerViewModel(
    val item: ContentItem,
    initialSection: Int,
    initialPage: Int,
    private val repository: McqRepository,
    private val preferences: AppPreferences,
    private val fileCache: PdfFileCache,
) : ViewModel() {

    private val documents = MutableStateFlow<Map<Int, DocumentState>>(emptyMap())
    private val search = PdfTextSearch()
    private var searchJob: Job? = null

    /** Page the reader returned to per section, so switching tabs keeps your place. */
    private val pagePerSection = mutableMapOf<Int, Int>()

    private val _position = MutableStateFlow(
        ViewerPosition(initialSection.coerceIn(0, (item.sections.size - 1).coerceAtLeast(0)), initialPage.coerceAtLeast(0)),
    )
    val position: StateFlow<ViewerPosition> = _position.asStateFlow()

    /** Bumped when the list must scroll programmatically (jump, search result, tab switch). */
    private val _scrollRequest = MutableStateFlow(ScrollRequest(_position.value.page, 0))
    val scrollRequest: StateFlow<ScrollRequest> = _scrollRequest.asStateFlow()

    val documentState: StateFlow<DocumentState> = _position
        .flatMapLatest { position -> documents.map { it[position.sectionIndex] ?: DocumentState.Loading } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DocumentState.Loading)

    val nightMode: StateFlow<Boolean> = preferences.nightMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val bookmarkedPages: StateFlow<Set<Int>> = _position
        .map { it.sectionIndex }
        .flatMapLatest { section -> repository.bookmarksForSection(item, section) }
        .map { bookmarks -> bookmarks.map { it.page }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    init {
        loadSection(_position.value.sectionIndex)
        _position
            .debounce(400)
            .onEach { preferences.saveLastRead(ReadingPosition(item, it.sectionIndex, it.page)) }
            .launchIn(viewModelScope)
    }

    fun selectSection(index: Int) {
        val current = _position.value
        if (index == current.sectionIndex || index !in item.sections.indices) return
        pagePerSection[current.sectionIndex] = current.page
        val page = pagePerSection[index] ?: 0
        _position.value = ViewerPosition(index, page)
        _searchState.value = SearchState.Idle
        requestScroll(page)
        loadSection(index)
    }

    fun onPageVisible(page: Int) {
        _position.update { if (it.page == page) it else it.copy(page = page) }
    }

    fun jumpToPage(page: Int) {
        _position.update { it.copy(page = page) }
        requestScroll(page)
    }

    fun retry() = loadSection(_position.value.sectionIndex, force = true)

    fun toggleNightMode() {
        viewModelScope.launch { preferences.setNightMode(!nightMode.value) }
    }

    /** Returns true if the page is now bookmarked. */
    fun toggleBookmark(): Boolean {
        val (section, page) = _position.value
        val bookmarked = page in bookmarkedPages.value
        viewModelScope.launch {
            if (bookmarked) repository.removeBookmark(item, section, page) else repository.addBookmark(item, section, page)
        }
        return !bookmarked
    }

    fun search(query: String) {
        val state = documentState.value as? DocumentState.Ready ?: return
        searchJob?.cancel()
        _searchState.value = SearchState.Searching
        searchJob = viewModelScope.launch {
            val pages = runCatching { search.search(state.renderer.file, query) }.getOrDefault(emptyList())
            _searchState.value = SearchState.Results(query, pages)
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchState.value = SearchState.Idle
    }

    private fun requestScroll(page: Int) {
        _scrollRequest.update { ScrollRequest(page, it.token + 1) }
    }

    private fun loadSection(index: Int, force: Boolean = false) {
        val existing = documents.value[index]
        if (!force && existing != null && existing != DocumentState.Error) return
        val url = item.sections.getOrNull(index)?.url ?: run {
            documents.update { it + (index to DocumentState.Error) }
            return
        }
        documents.update { it + (index to DocumentState.Loading) }
        viewModelScope.launch {
            val state = runCatching { PdfDocumentRenderer.open(fileCache.get(url)) }
                .fold(onSuccess = { DocumentState.Ready(it) }, onFailure = { DocumentState.Error })
            documents.update { it + (index to state) }
        }
    }

    override fun onCleared() {
        documents.value.values.filterIsInstance<DocumentState.Ready>().forEach { it.renderer.close() }
    }
}

data class ScrollRequest(val page: Int, val token: Int)
