package com.blackcall.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackcall.data.BlackCallRepository
import com.blackcall.data.db.CallLogEntry
import com.blackcall.domain.FilterSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class HomeUiState(
    val settings: FilterSettings = FilterSettings(),
    val today: Int = 0,
    val week: Int = 0,
    val total: Int = 0,
    /** Oldest first; the last entry is today. */
    val last7: List<Int> = List(7) { 0 },
    val dayLabels: List<String> = List(7) { "" },
    val recent: List<CallLogEntry> = emptyList()
)

class HomeViewModel(private val repo: BlackCallRepository) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(repo.settings, repo.callLog) { settings, log ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val perDay = log.groupingBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }.eachCount()
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val counts = days.map { perDay[it] ?: 0 }
        HomeUiState(
            settings = settings,
            today = counts.last(),
            week = counts.sum(),
            total = log.size,
            last7 = counts,
            dayLabels = days.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3) },
            recent = log.take(4)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun update(transform: (FilterSettings) -> FilterSettings) {
        viewModelScope.launch { repo.settingsStore.update(transform) }
    }
}
