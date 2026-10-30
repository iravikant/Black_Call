package com.blackcall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.blackcall.domain.RuleType
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM number_rules WHERE type = :type ORDER BY createdAt DESC")
    fun observe(type: RuleType): Flow<List<NumberRule>>

    @Query("SELECT * FROM number_rules")
    fun observeAll(): Flow<List<NumberRule>>

    @Query("SELECT * FROM number_rules")
    suspend fun getAll(): List<NumberRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: NumberRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<NumberRule>)

    @Query("DELETE FROM number_rules WHERE normalized = :normalized")
    suspend fun delete(normalized: String)

    @Query("DELETE FROM number_rules WHERE type = :type")
    suspend fun clear(type: RuleType)
}

@Dao
interface PrefixDao {
    @Query("SELECT * FROM prefix_rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PrefixRule>>

    @Query("SELECT * FROM prefix_rules")
    suspend fun getAll(): List<PrefixRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: PrefixRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<PrefixRule>)

    @Query("DELETE FROM prefix_rules WHERE prefix = :prefix")
    suspend fun delete(prefix: String)

    @Query("DELETE FROM prefix_rules")
    suspend fun clear()
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CallLogEntry>>

    @Insert
    suspend fun insert(entry: CallLogEntry): Long

    @Query("DELETE FROM call_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM call_log")
    suspend fun clear()

    /** Keeps the newest [keep] rows so the log can't grow without bound. */
    @Query("DELETE FROM call_log WHERE id NOT IN (SELECT id FROM call_log ORDER BY timestamp DESC LIMIT :keep)")
    suspend fun prune(keep: Int)
}
