package com.blackcall.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NumberRule::class, PrefixRule::class, CallLogEntry::class],
    version = 1,
    exportSchema = true
)
abstract class BlackCallDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun prefixDao(): PrefixDao
    abstract fun callLogDao(): CallLogDao

    companion object {
        fun create(context: Context): BlackCallDatabase =
            Room.databaseBuilder(context.applicationContext, BlackCallDatabase::class.java, "blackcall.db")
                .build()
    }
}
