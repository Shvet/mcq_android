package com.onegoal.mcq.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onegoal.mcq.data.McqRepository
import com.onegoal.mcq.data.prefs.AppPreferences
import com.onegoal.mcq.model.AppConfig
import com.onegoal.mcq.model.ReadingPosition
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Activity-wide state: admin-panel config, bookmark badge count and the last-read shortcut. */
class AppViewModel(
    private val repository: McqRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    val config: StateFlow<AppConfig> = preferences.appConfig
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppConfig())

    val bookmarkCount: StateFlow<Int> = repository.bookmarkCount
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val lastRead: StateFlow<ReadingPosition?> = preferences.lastRead
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { repository.refreshAppConfig() }
    }

    suspend fun isLastReadHintShown(): Boolean = preferences.isLastReadHintShown()

    fun markLastReadHintShown() {
        viewModelScope.launch { preferences.setLastReadHintShown() }
    }

    /** Returns true when an interstitial should be shown before opening a PDF. */
    suspend fun registerPdfOpen(): Boolean {
        val current = config.value
        if (!current.adsEnabled || current.interstitialEvery <= 0) return false
        return preferences.incrementPdfOpenCount() % current.interstitialEvery == 0
    }
}
