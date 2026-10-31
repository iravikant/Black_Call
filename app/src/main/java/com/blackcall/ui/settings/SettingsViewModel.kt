package com.blackcall.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackcall.data.BlackCallRepository
import com.blackcall.domain.FilterSettings
import com.blackcall.domain.Profile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repo: BlackCallRepository) : ViewModel() {

    val settings: StateFlow<FilterSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FilterSettings())

    val profile: StateFlow<Profile> = repo.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Profile())

    fun update(transform: (FilterSettings) -> FilterSettings) {
        viewModelScope.launch { repo.settingsStore.update(transform) }
    }

    suspend fun exportCsv(): String = repo.exportCsv()

    /** Returns (imported, skipped). */
    suspend fun importCsv(text: String): Pair<Int, Int> = repo.importCsv(text)
}
