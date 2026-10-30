package com.blackcall.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.blackcall.domain.BlockAction
import com.blackcall.domain.BlockReason
import com.blackcall.domain.RuleType

/** A single number on the blocked or allowed list. One row per number, so it can't be on both. */
@Entity(tableName = "number_rules")
data class NumberRule(
    @PrimaryKey val normalized: String,
    /** The number as the user typed it, for display and export. */
    val number: String,
    val type: RuleType,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "prefix_rules")
data class PrefixRule(
    @PrimaryKey val prefix: String,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_log", indices = [Index("timestamp")])
data class CallLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Empty for hidden numbers. */
    val number: String,
    val normalized: String,
    val action: BlockAction,
    val reason: BlockReason,
    val score: Int = 0,
    /** Signals joined with '|'. */
    val signals: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

fun CallLogEntry.signalList(): List<String> =
    if (signals.isEmpty()) emptyList() else signals.split("|")
