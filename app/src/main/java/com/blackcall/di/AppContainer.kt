package com.blackcall.di

import android.content.Context
import com.blackcall.data.BlackCallRepository
import com.blackcall.data.ContactsLookup
import com.blackcall.data.db.BlackCallDatabase
import com.blackcall.data.prefs.SettingsStore
import com.blackcall.domain.BlockEngine
import com.blackcall.domain.RuleSnapshot
import com.blackcall.service.BlockNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Manual dependency container. Small enough that Hilt would be more ceremony than value. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsStore = SettingsStore(appContext)
    val repository = BlackCallRepository(BlackCallDatabase.create(appContext), settingsStore)
    val contacts = ContactsLookup(appContext)
    val engine = BlockEngine(contacts::isContact)
    val notifier = BlockNotifier(appContext)

    /**
     * Rules and settings held in memory, kept current by Room/DataStore flows.
     * The screening service reads this instead of hitting the database on every call.
     */
    val snapshot: StateFlow<RuleSnapshot?> = repository.snapshot.stateIn(
        appScope, SharingStarted.Eagerly, null as RuleSnapshot?
    )
}
