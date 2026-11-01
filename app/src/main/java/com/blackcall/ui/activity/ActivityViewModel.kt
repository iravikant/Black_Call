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

/** All blocked calls from one number, collapsed into a single row. */
data class LogGroup(
    val latest: CallLogEntry,   // most recent call from this number
    val count: Int,             // how many times it was blocked
    val ids: List<Long>         // every row in the group, for deleting
)

class ActivityViewModel(private val repo: BlackCallRepository) : ViewModel() {

    val filter = MutableStateFlow(LogFilter.ALL)

    val groups: StateFlow<List<LogGroup>> = combine(repo.callLog, filter) { log, f ->
        log.filter { f.matches(it.reason) }
            .groupBy { it.normalized }          // hidden numbers (empty) group together
            .values
            .map { rows ->
                LogGroup(
                    latest = rows.maxBy { it.timestamp },
                    count = rows.size,
                    ids = rows.map { it.id }
                )
            }
            .sortedByDescending { it.latest.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Total blocked calls (not groups), so the header count stays accurate. */
    val totalCount: StateFlow<Int> = repo.callLog
        .combine(filter) { log, _ -> log.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setFilter(f: LogFilter) { filter.value = f }

    fun block(entry: CallLogEntry) {
        viewModelScope.launch { repo.saveRule(entry.number, RuleType.BLOCK) }
    }

    fun allow(entry: CallLogEntry) {
        viewModelScope.launch { repo.saveRule(entry.number, RuleType.ALLOW) }
    }

    fun deleteGroup(group: LogGroup) {
        viewModelScope.launch { group.ids.forEach { repo.deleteLog(it) } }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clearLog() }
    }
}