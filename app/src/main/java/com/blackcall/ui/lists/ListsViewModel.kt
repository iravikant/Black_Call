package com.blackcall.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackcall.data.BlackCallRepository
import com.blackcall.data.db.NumberRule
import com.blackcall.data.db.PrefixRule
import com.blackcall.domain.RuleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ListTab(val title: String) {
    BLOCKED("Blocked"),
    ALLOWED("Allowed"),
    PREFIXES("Prefixes")
}

data class ListsUiState(
    val blocked: List<NumberRule> = emptyList(),
    val allowed: List<NumberRule> = emptyList(),
    val prefixes: List<PrefixRule> = emptyList()
)

class ListsViewModel(private val repo: BlackCallRepository) : ViewModel() {

    val tab = MutableStateFlow(ListTab.BLOCKED)
    val query = MutableStateFlow("")

    val state: StateFlow<ListsUiState> = combine(
        repo.blockedRules, repo.allowedRules, repo.prefixRules, query
    ) { blocked, allowed, prefixes, q ->
        val needle = q.trim().lowercase()
        fun NumberRule.matches() = needle.isEmpty() || number.contains(needle) || label.lowercase().contains(needle)
        ListsUiState(
            blocked = blocked.filter { it.matches() },
            allowed = allowed.filter { it.matches() },
            prefixes = prefixes.filter { needle.isEmpty() || it.prefix.contains(needle) || it.label.lowercase().contains(needle) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListsUiState())

    fun selectTab(t: ListTab) { tab.value = t }
    fun setQuery(q: String) { query.update { q } }

    /** Returns true when the entry was valid and saved. */
    suspend fun add(target: ListTab, value: String, label: String): Boolean = when (target) {
        ListTab.BLOCKED -> repo.saveRule(value, RuleType.BLOCK, label)
        ListTab.ALLOWED -> repo.saveRule(value, RuleType.ALLOW, label)
        ListTab.PREFIXES -> repo.savePrefix(value, label)
    }

    fun remove(rule: NumberRule) { viewModelScope.launch { repo.removeRule(rule) } }
    fun restore(rule: NumberRule) { viewModelScope.launch { repo.restoreRule(rule) } }
    fun remove(rule: PrefixRule) { viewModelScope.launch { repo.removePrefix(rule) } }
    fun restore(rule: PrefixRule) { viewModelScope.launch { repo.restorePrefix(rule) } }
}
