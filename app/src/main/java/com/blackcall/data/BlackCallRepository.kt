package com.blackcall.data

import com.blackcall.data.db.BlackCallDatabase
import com.blackcall.data.db.CallLogEntry
import com.blackcall.data.db.NumberRule
import com.blackcall.data.db.PrefixRule
import com.blackcall.data.prefs.SettingsStore
import com.blackcall.domain.BackupEntry
import com.blackcall.domain.BlockAction
import com.blackcall.domain.BlockReason
import com.blackcall.domain.CsvBackup
import com.blackcall.domain.Decision
import com.blackcall.domain.FilterSettings
import com.blackcall.domain.PhoneNumbers
import com.blackcall.domain.RuleSnapshot
import com.blackcall.domain.RuleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BlackCallRepository(
    private val db: BlackCallDatabase,
    val settingsStore: SettingsStore
) {
    private val rules = db.ruleDao()
    private val prefixDao = db.prefixDao()
    private val logDao = db.callLogDao()

    val settings: Flow<FilterSettings> = settingsStore.settings
    val blockedRules: Flow<List<NumberRule>> = rules.observe(RuleType.BLOCK)
    val allowedRules: Flow<List<NumberRule>> = rules.observe(RuleType.ALLOW)
    val prefixRules: Flow<List<PrefixRule>> = prefixDao.observeAll()
    val callLog: Flow<List<CallLogEntry>> = logDao.observeAll()

    /** Everything the screening engine needs, kept hot in memory by [com.blackcall.di.AppContainer]. */
    val snapshot: Flow<RuleSnapshot> = combine(
        settings, rules.observeAll(), prefixDao.observeAll()
    ) { s, all, prefixes ->
        RuleSnapshot(
            settings = s,
            blocked = all.filter { it.type == RuleType.BLOCK }.map { it.normalized }.toSet(),
            allowed = all.filter { it.type == RuleType.ALLOW }.map { it.normalized }.toSet(),
            prefixes = prefixes.map { it.prefix }
        )
    }

    /** Adding a number to one list moves it off the other. Returns false if the number is invalid. */
    suspend fun saveRule(number: String, type: RuleType, label: String = ""): Boolean {
        if (!PhoneNumbers.isValidNumber(number)) return false
        rules.upsert(NumberRule(PhoneNumbers.key(number), number.trim(), type, label.trim()))
        return true
    }

    suspend fun restoreRule(rule: NumberRule) = rules.upsert(rule)
    suspend fun removeRule(rule: NumberRule) = rules.delete(rule.normalized)
    suspend fun clearRules(type: RuleType) = rules.clear(type)

    suspend fun savePrefix(raw: String, label: String = ""): Boolean {
        val prefix = PhoneNumbers.normalizePrefix(raw) ?: return false
        prefixDao.upsert(PrefixRule(prefix, label.trim()))
        return true
    }

    suspend fun restorePrefix(rule: PrefixRule) = prefixDao.upsert(rule)
    suspend fun removePrefix(rule: PrefixRule) = prefixDao.delete(rule.prefix)

    suspend fun logBlock(rawNumber: String?, action: BlockAction, decision: Decision.Block) {
        logDao.insert(
            CallLogEntry(
                number = rawNumber.orEmpty(),
                normalized = rawNumber?.let(PhoneNumbers::key).orEmpty(),
                action = action,
                reason = decision.reason,
                score = decision.score,
                signals = decision.signals.joinToString("|")
            )
        )
        logDao.prune(MAX_LOG_ROWS)
    }

    suspend fun deleteLog(id: Long) = logDao.delete(id)
    suspend fun clearLog() = logDao.clear()

    // ---- Backup -------------------------------------------------------------------------------

    suspend fun exportCsv(): String {
        val numbers = rules.getAll().sortedBy { it.createdAt }.map {
            BackupEntry(it.type.name, it.number, it.label)
        }
        val prefixes = prefixDao.getAll().sortedBy { it.createdAt }.map {
            BackupEntry("PREFIX", it.prefix, it.label)
        }
        return CsvBackup.export(numbers + prefixes)
    }

    /** Returns (imported, skipped). */
    suspend fun importCsv(text: String): Pair<Int, Int> {
        val parsed = CsvBackup.parse(text)
        var imported = 0
        for (e in parsed.entries) {
            val ok = when (e.kind) {
                "BLOCK" -> saveRule(e.value, RuleType.BLOCK, e.label)
                "ALLOW" -> saveRule(e.value, RuleType.ALLOW, e.label)
                else -> savePrefix(e.value, e.label)
            }
            if (ok) imported++
        }
        return imported to (parsed.skipped + parsed.entries.size - imported)
    }

    companion object {
        const val MAX_LOG_ROWS = 1000
    }
}
