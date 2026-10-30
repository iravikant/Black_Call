package com.blackcall.permissions

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.blackcall.container
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class Importance { REQUIRED, RECOMMENDED, OPTIONAL }

enum class AppPermission(
    val title: String,
    val why: String,
    val importance: Importance
) {
    CALL_SCREENING(
        "Call screening",
        "Lets Black Call check each incoming call and stop unwanted ones before they ring.",
        Importance.REQUIRED
    ),
    CONTACTS(
        "Contacts",
        "Recognises people you know so they are never blocked. Stays on your phone, never uploaded.",
        Importance.RECOMMENDED
    ),
    NOTIFICATIONS(
        "Notifications",
        "Tells you when a call is blocked, so nothing important slips by unnoticed.",
        Importance.OPTIONAL
    )
}

enum class PermState {
    GRANTED,
    /** Not granted, but the system dialog can still be shown. */
    DENIED,
    /** User chose "don't ask again": only system settings can fix it. */
    BLOCKED,
    /** The device doesn't offer this (e.g. no call screening role). */
    UNAVAILABLE
}

data class PermissionStatus(val permission: AppPermission, val state: PermState) {
    val granted get() = state == PermState.GRANTED
}

/**
 * Single place that knows how to check and request every permission, including the
 * fallbacks when Android will no longer show a dialog.
 */
@Stable
class PermissionController internal constructor(
    private val context: Context,
    private val tick: MutableIntState,
    private val asked: State<Set<String>>,
    private val markAsked: (String) -> Unit,
    private val runtimeLauncher: ManagedActivityResultLauncher<String, Boolean>,
    private val roleLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    /** Recomputed whenever a result comes back or the app resumes (see [tick]). */
    val statuses: List<PermissionStatus>
        get() {
            tick.intValue
            return AppPermission.entries.map { PermissionStatus(it, stateOf(it)) }
        }

    fun state(p: AppPermission): PermState = statuses.first { it.permission == p }.state

    val allRequiredGranted: Boolean
        get() = statuses.filter { it.permission.importance == Importance.REQUIRED }.all { it.granted }

    /** True when tapping "Allow" can only send the user to system settings. */
    fun needsSettings(p: AppPermission): Boolean {
        val s = state(p)
        return s == PermState.BLOCKED || (p == AppPermission.CALL_SCREENING && s == PermState.DENIED && p.name in asked.value)
    }

    fun request(p: AppPermission) {
        val useSettings = needsSettings(p)
        markAsked(p.name)
        when (p) {
            AppPermission.CALL_SCREENING -> {
                val rm = context.getSystemService(RoleManager::class.java)
                if (useSettings || rm == null) {
                    openSafely(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                } else {
                    roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                }
            }
            AppPermission.CONTACTS ->
                if (useSettings) openAppSettings() else runtimeLauncher.launch(Manifest.permission.READ_CONTACTS)
            AppPermission.NOTIFICATIONS ->
                if (useSettings || Build.VERSION.SDK_INT < 33) {
                    openSafely(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                } else {
                    runtimeLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
        }
    }

    fun openAppSettings() = openSafely(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    )

    private fun openSafely(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Some OEM builds lack individual settings screens; app details always exists.
            if (intent.action != Settings.ACTION_APPLICATION_DETAILS_SETTINGS) openAppSettings()
        }
    }

    private fun stateOf(p: AppPermission): PermState = when (p) {
        AppPermission.CALL_SCREENING -> {
            val rm = context.getSystemService(RoleManager::class.java)
            when {
                rm == null || !rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) -> PermState.UNAVAILABLE
                rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) -> PermState.GRANTED
                else -> PermState.DENIED
            }
        }
        AppPermission.CONTACTS -> runtimeState(p, Manifest.permission.READ_CONTACTS)
        AppPermission.NOTIFICATIONS ->
            if (Build.VERSION.SDK_INT >= 33) runtimeState(p, Manifest.permission.POST_NOTIFICATIONS)
            else if (NotificationManagerCompat.from(context).areNotificationsEnabled()) PermState.GRANTED
            else PermState.BLOCKED
    }

    private fun runtimeState(p: AppPermission, permission: String): PermState {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return PermState.GRANTED
        }
        val activity = context.findActivity()
        val canShowDialog = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        // Asked before, and Android won't show the dialog again => permanently denied.
        return if (p.name in asked.value && !canShowDialog) PermState.BLOCKED else PermState.DENIED
    }
}

@Composable
fun rememberPermissionController(): PermissionController {
    val context = LocalContext.current
    val store = remember { context.container.settingsStore }
    val scope = rememberCoroutineScope()
    val tick = remember { mutableIntStateOf(0) }
    val asked = remember(store) { store.flags.map { it.askedPermissions } }.collectAsState(initial = emptySet())

    val runtimeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        tick.intValue++
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        tick.intValue++
    }

    // The user may flip permissions in system settings and come back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick.intValue++ }

    return remember(context, tick, asked, runtimeLauncher, roleLauncher) {
        PermissionController(
            context = context,
            tick = tick,
            asked = asked,
            markAsked = { name -> scope.launch { store.markPermissionAsked(name) } },
            runtimeLauncher = runtimeLauncher,
            roleLauncher = roleLauncher
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
