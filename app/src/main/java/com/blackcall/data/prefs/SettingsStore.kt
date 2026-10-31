package com.blackcall.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.blackcall.domain.AccentId
import com.blackcall.domain.Appearance
import com.blackcall.domain.BlockAction
import com.blackcall.domain.FilterSettings
import com.blackcall.domain.Profile
import com.blackcall.domain.Sensitivity
import com.blackcall.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "black_call_settings")

data class AppFlags(
    val onboardingDone: Boolean = false,
    /** Permissions we have already asked for; lets us tell "never asked" from "permanently denied". */
    val askedPermissions: Set<String> = emptySet()
)

class SettingsStore(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    private object K {
        val enabled = booleanPreferencesKey("protection_enabled")
        val contactsOnly = booleanPreferencesKey("contacts_only")
        val blockUnknown = booleanPreferencesKey("block_unknown")
        val blockHidden = booleanPreferencesKey("block_hidden")
        val blockSpoofed = booleanPreferencesKey("block_spoofed")
        val blockSpam = booleanPreferencesKey("block_spam")
        val sensitivity = stringPreferencesKey("sensitivity")
        val action = stringPreferencesKey("block_action")
        val quietEnabled = booleanPreferencesKey("quiet_enabled")
        val quietStart = intPreferencesKey("quiet_start")
        val quietEnd = intPreferencesKey("quiet_end")
        val notify = booleanPreferencesKey("notify_on_block")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val asked = stringSetPreferencesKey("asked_permissions")
        val themeMode = stringPreferencesKey("theme_mode")
        val amoled = booleanPreferencesKey("amoled")
        val accent = stringPreferencesKey("accent")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val profileName = stringPreferencesKey("profile_name")
    }

    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<FilterSettings> = data.map { it.toSettings() }

    val flags: Flow<AppFlags> = data.map {
        AppFlags(
            onboardingDone = it[K.onboardingDone] ?: false,
            askedPermissions = it[K.asked] ?: emptySet()
        )
    }

    val appearance: Flow<Appearance> = data.map {
        val d = Appearance()
        Appearance(
            mode = it[K.themeMode]?.let { v -> runCatching { ThemeMode.valueOf(v) }.getOrNull() } ?: d.mode,
            amoled = it[K.amoled] ?: d.amoled,
            accent = it[K.accent]?.let { v -> runCatching { AccentId.valueOf(v) }.getOrNull() } ?: d.accent,
            reduceMotion = it[K.reduceMotion] ?: d.reduceMotion
        )
    }

    val profile: Flow<Profile> = data.map { Profile(it[K.profileName].orEmpty()) }

    suspend fun updateAppearance(transform: (Appearance) -> Appearance) {
        store.edit { prefs ->
            val d = Appearance()
            val current = Appearance(
                mode = prefs[K.themeMode]?.let { v -> runCatching { ThemeMode.valueOf(v) }.getOrNull() } ?: d.mode,
                amoled = prefs[K.amoled] ?: d.amoled,
                accent = prefs[K.accent]?.let { v -> runCatching { AccentId.valueOf(v) }.getOrNull() } ?: d.accent,
                reduceMotion = prefs[K.reduceMotion] ?: d.reduceMotion
            )
            val a = transform(current)
            prefs[K.themeMode] = a.mode.name
            prefs[K.amoled] = a.amoled
            prefs[K.accent] = a.accent.name
            prefs[K.reduceMotion] = a.reduceMotion
        }
    }

    suspend fun setProfileName(name: String) {
        store.edit { it[K.profileName] = name.trim().take(Profile.MAX_NAME) }
    }

    suspend fun update(transform: (FilterSettings) -> FilterSettings) {
        store.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[K.enabled] = s.protectionEnabled
            prefs[K.contactsOnly] = s.contactsOnly
            prefs[K.blockUnknown] = s.blockUnknown
            prefs[K.blockHidden] = s.blockHidden
            prefs[K.blockSpoofed] = s.blockSpoofed
            prefs[K.blockSpam] = s.blockSpam
            prefs[K.sensitivity] = s.sensitivity.name
            prefs[K.action] = s.action.name
            prefs[K.quietEnabled] = s.quietHoursEnabled
            prefs[K.quietStart] = s.quietStartMinutes
            prefs[K.quietEnd] = s.quietEndMinutes
            prefs[K.notify] = s.notifyOnBlock
        }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        store.edit { it[K.onboardingDone] = done }
    }

    suspend fun markPermissionAsked(name: String) {
        store.edit { it[K.asked] = (it[K.asked] ?: emptySet()) + name }
    }

    private fun Preferences.toSettings(): FilterSettings {
        val d = FilterSettings()
        return FilterSettings(
            protectionEnabled = this[K.enabled] ?: d.protectionEnabled,
            contactsOnly = this[K.contactsOnly] ?: d.contactsOnly,
            blockUnknown = this[K.blockUnknown] ?: d.blockUnknown,
            blockHidden = this[K.blockHidden] ?: d.blockHidden,
            blockSpoofed = this[K.blockSpoofed] ?: d.blockSpoofed,
            blockSpam = this[K.blockSpam] ?: d.blockSpam,
            sensitivity = this[K.sensitivity]?.let { runCatching { Sensitivity.valueOf(it) }.getOrNull() } ?: d.sensitivity,
            action = this[K.action]?.let { runCatching { BlockAction.valueOf(it) }.getOrNull() } ?: d.action,
            quietHoursEnabled = this[K.quietEnabled] ?: d.quietHoursEnabled,
            quietStartMinutes = this[K.quietStart] ?: d.quietStartMinutes,
            quietEndMinutes = this[K.quietEnd] ?: d.quietEndMinutes,
            notifyOnBlock = this[K.notify] ?: d.notifyOnBlock
        )
    }
}
