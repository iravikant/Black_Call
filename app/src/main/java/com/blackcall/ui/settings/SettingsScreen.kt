package com.blackcall.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blackcall.domain.BlockAction
import com.blackcall.domain.Sensitivity
import com.blackcall.permissions.AppPermission
import com.blackcall.permissions.Importance
import com.blackcall.permissions.PermState
import com.blackcall.permissions.PermissionController
import com.blackcall.ui.Format
import com.blackcall.ui.LocalSnackbar
import com.blackcall.ui.appViewModel
import com.blackcall.ui.components.GhostButton
import com.blackcall.ui.components.GlassCard
import com.blackcall.ui.components.GradientButton
import com.blackcall.ui.components.GradientText
import com.blackcall.ui.components.IconBadge
import com.blackcall.ui.components.Pill
import com.blackcall.ui.components.SectionHeader
import com.blackcall.ui.components.SegmentedControl
import com.blackcall.ui.components.SettingSwitchRow
import com.blackcall.ui.components.Staggered
import com.blackcall.ui.components.TimePickerDialog
import com.blackcall.ui.theme.Coral
import com.blackcall.ui.theme.GreenBright
import com.blackcall.ui.theme.TealBright
import com.blackcall.ui.theme.TextPrimary
import com.blackcall.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(
    permissions: PermissionController,
    contentPadding: PaddingValues,
    vm: SettingsViewModel = appViewModel { SettingsViewModel(it.repository) }
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }

    fun say(msg: String) = scope.launch {
        snackbar.currentSnackbarData?.dismiss()
        snackbar.showSnackbar(msg)
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText().take(2_000_000) }
                }.getOrNull()
            }
            if (text == null) say("Couldn't read that file")
            else {
                val (ok, skipped) = vm.importCsv(text)
                say(if (skipped > 0) "Imported $ok, skipped $skipped invalid" else "Imported $ok entries")
            }
        }
    }

    fun export() {
        scope.launch {
            val csv = vm.exportCsv()
            val file = withContext(Dispatchers.IO) {
                File(context.cacheDir, "exports").apply { mkdirs() }
                    .resolve("black-call-lists.csv").also { it.writeText(csv) }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Export lists"))
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Staggered(0) {
                Column {
                    GradientText("Settings", MaterialTheme.typography.headlineLarge)
                    Text("Tune how Black Call protects you.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }

        item {
            Staggered(1) {
                GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp, 8.dp)) {
                    SectionHeader("Filters")
                    SettingSwitchRow(Icons.Rounded.Report, "Spam detection", "Score numbers for spam patterns",
                        s.blockSpam, { v -> vm.update { it.copy(blockSpam = v) } })
                    AnimatedVisibility(s.blockSpam) {
                        Column(Modifier.padding(bottom = 8.dp)) {
                            SegmentedControl(Sensitivity.entries, s.sensitivity, { it.label }, { v -> vm.update { it.copy(sensitivity = v) } })
                            Text(
                                when (s.sensitivity) {
                                    Sensitivity.RELAXED -> "Only blocks obvious spam. Fewest false alarms."
                                    Sensitivity.BALANCED -> "Recommended mix of catching spam and letting calls through."
                                    Sensitivity.STRICT -> "Catches more, but may block the odd legitimate call."
                                },
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    SettingSwitchRow(Icons.Rounded.GppBad, "Spoofed callers", "Block numbers your carrier couldn't verify",
                        s.blockSpoofed, { v -> vm.update { it.copy(blockSpoofed = v) } })
                    SettingSwitchRow(Icons.Rounded.PersonOff, "Unknown callers", "Block numbers not in your contacts",
                        s.blockUnknown, { v -> vm.update { it.copy(blockUnknown = v) } })
                    SettingSwitchRow(Icons.Rounded.VisibilityOff, "Hidden numbers", "Block callers who hide their number",
                        s.blockHidden, { v -> vm.update { it.copy(blockHidden = v) } })
                    SettingSwitchRow(Icons.Rounded.Contacts, "Contacts only", "Only contacts and allowed numbers can ring",
                        s.contactsOnly, { v -> vm.update { it.copy(contactsOnly = v) } })
                }
            }
        }

        item {
            Staggered(2) {
                GlassCard(Modifier.fillMaxWidth().animateContentSize()) {
                    SectionHeader("When a call is blocked")
                    SegmentedControl(BlockAction.entries, s.action, { it.label }, { v -> vm.update { it.copy(action = v) } })
                    Text(
                        if (s.action == BlockAction.REJECT) "The caller is hung up on. Your phone never rings."
                        else "Your phone stays quiet, but the call shows as missed so you can call back.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    SettingSwitchRow(Icons.Rounded.Notifications, "Notify me", "Show a notification for each blocked call",
                        s.notifyOnBlock, { v -> vm.update { it.copy(notifyOnBlock = v) } })
                }
            }
        }

        item {
            Staggered(3) {
                GlassCard(Modifier.fillMaxWidth().animateContentSize()) {
                    SettingSwitchRow(Icons.Rounded.Bedtime, "Quiet hours", "Only contacts and allowed numbers ring during this window",
                        s.quietHoursEnabled, { v -> vm.update { it.copy(quietHoursEnabled = v) } })
                    AnimatedVisibility(s.quietHoursEnabled) {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TimeChip("From", Format.minuteOfDay(s.quietStartMinutes), Modifier.weight(1f)) { pickStart = true }
                            TimeChip("Until", Format.minuteOfDay(s.quietEndMinutes), Modifier.weight(1f)) { pickEnd = true }
                        }
                    }
                }
            }
        }

        item {
            Staggered(4) {
                GlassCard(Modifier.fillMaxWidth()) {
                    SectionHeader("Permissions")
                    permissions.statuses.forEach { st ->
                        PermissionRow(st.permission, st.state, permissions.needsSettings(st.permission)) {
                            permissions.request(st.permission)
                        }
                    }
                }
            }
        }

        item {
            Staggered(5) {
                GlassCard(Modifier.fillMaxWidth()) {
                    SectionHeader("Backup")
                    Text(
                        "Export your blocked list, allowed list and prefixes as a CSV file, or restore them on a new phone.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradientButton("Export", { export() }, Modifier.weight(1f))
                        GhostButton("Import", { importer.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) }, Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Staggered(6) {
                GlassCard(Modifier.fillMaxWidth()) {
                    SectionHeader("Privacy")
                    Text(
                        "Everything happens on your phone. Black Call has no internet permission, so your contacts, lists and call history can't leave the device.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                    Text(
                        "Version 2.0",
                        style = MaterialTheme.typography.labelMedium, color = TextSecondary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }

    if (pickStart) {
        TimePickerDialog("Quiet hours start", s.quietStartMinutes,
            onConfirm = { m -> vm.update { it.copy(quietStartMinutes = m) }; pickStart = false },
            onDismiss = { pickStart = false })
    }
    if (pickEnd) {
        TimePickerDialog("Quiet hours end", s.quietEndMinutes,
            onConfirm = { m -> vm.update { it.copy(quietEndMinutes = m) }; pickEnd = false },
            onDismiss = { pickEnd = false })
    }
}

@Composable
private fun TimeChip(label: String, time: String, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier, onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(time, style = MaterialTheme.typography.titleMedium, color = TealBright)
    }
}

@Composable
fun PermissionRow(
    permission: AppPermission,
    state: PermState,
    needsSettings: Boolean,
    onRequest: () -> Unit
) {
    val granted = state == PermState.GRANTED
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !granted && state != PermState.UNAVAILABLE, onClick = onRequest)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            when (permission) {
                AppPermission.CALL_SCREENING -> Icons.Rounded.Phone
                AppPermission.CONTACTS -> Icons.Rounded.Contacts
                AppPermission.NOTIFICATIONS -> Icons.Rounded.Notifications
            }
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(permission.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(permission.why, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        when {
            granted -> Pill("Allowed", color = GreenBright)
            state == PermState.UNAVAILABLE -> Pill("Unavailable", color = TextSecondary)
            else -> Pill(
                if (needsSettings) "Open settings" else if (permission.importance == Importance.REQUIRED) "Required" else "Allow",
                color = if (permission.importance == Importance.REQUIRED) Coral else TealBright
            )
        }
    }
}
