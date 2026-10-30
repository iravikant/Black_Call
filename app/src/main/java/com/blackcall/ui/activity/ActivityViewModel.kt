package com.blackcall.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackcall.data.BlackCallRepository
import com.blackcall.data.db.CallLogEntry
import com.blackcall.domain.BlockReason
import com.blackcall.domain.RuleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LogFilter(val title: String, private val reasons: Set<BlockReason>?) {
    ALL("All", null),
    SPAM("Spam", setOf(BlockReason.SPAM, BlockReason.SPOOFED)),
    UNKNOWN("Unknown", setOf(BlockReason.UNKNOWN, BlockReason.HIDDEN, BlockReason.NOT_ALLOWED)),
    RULES("Rules", setOf(BlockReason.MANUAL, BlockReason.PREFIX, BlockReason.SCHEDULE));

    fun matches(reason: BlockReason) = reasons == null || reason in reasons
}

class ActivityViewModel(private val repo: BlackCallRepository) : ViewModel() {

    val filter = MutableStateFlow(LogFilter.ALL)

    val entries: StateFlow<List<CallLogEntry>> = combine(repo.callLog, filter) { log, f ->
        log.filter { f.matches(it.reason) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalCount: StateFlow<Int> = combine(repo.callLog, filter) { log, _ -> log.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setFilter(f: LogFilter) { filter.value = f }

    fun block(entry: CallLogEntry) {
        viewModelScope.launch { repo.saveRule(entry.number, RuleType.BLOCK) }
    }

    fun allow(entry: CallLogEntry) {
        viewModelScope.launch { repo.saveRule(entry.number, RuleType.ALLOW) }
    }

    fun delete(entry: CallLogEntry) {
        viewModelScope.launch { repo.deleteLog(entry.id) }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clearLog() }
    }
}
