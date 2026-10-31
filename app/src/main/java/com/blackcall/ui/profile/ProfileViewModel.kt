package com.blackcall.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackcall.data.BlackCallRepository
import com.blackcall.domain.Appearance
import com.blackcall.domain.Profile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: Profile = Profile(),
    val appearance: Appearance = Appearance(),
    val blockedCalls: Int = 0,
    val blockedNumbers: Int = 0,
    val allowedNumbers: Int = 0,
    val daysProtected: Int = 1
)

class ProfileViewModel(
    private val repo: BlackCallRepository,
    private val installTime: Long
) : ViewModel() {

    val state: StateFlow<ProfileUiState> = combine(
        repo.profile, repo.appearance, repo.callLog, repo.blockedRules, repo.allowedRules
    ) { profile, appearance, log, blocked, allowed ->
        val days = ((System.currentTimeMillis() - installTime) / 86_400_000L).toInt() + 1
        ProfileUiState(profile, appearance, log.size, blocked.size, allowed.size, days.coerceAtLeast(1))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun setName(name: String) { viewModelScope.launch { repo.settingsStore.setProfileName(name) } }

    fun updateAppearance(transform: (Appearance) -> Appearance) {
        viewModelScope.launch { repo.settingsStore.updateAppearance(transform) }
    }
}
