package com.blackcall.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blackcall.R
import com.blackcall.domain.FilterSettings
import com.blackcall.permissions.AppPermission
import com.blackcall.permissions.Importance
import com.blackcall.permissions.PermState
import com.blackcall.permissions.PermissionController
import com.blackcall.permissions.PermissionStatus
import com.blackcall.ui.activity.CallLogRow
import com.blackcall.ui.appViewModel
import com.blackcall.ui.components.AnimatedCounter
import com.blackcall.ui.components.Avatar
import com.blackcall.ui.components.EmptyState
import com.blackcall.ui.components.GhostButton
import com.blackcall.ui.components.GlassCard
import com.blackcall.ui.components.GradientButton
import com.blackcall.ui.components.GradientText
import com.blackcall.ui.components.SectionHeader
import com.blackcall.ui.components.SegmentedControl
import com.blackcall.ui.components.SettingSwitchRow
import com.blackcall.ui.components.ShieldHero
import com.blackcall.ui.components.Staggered
import com.blackcall.ui.components.WeekBarChart
import com.blackcall.ui.lists.CallRow
import com.blackcall.ui.lists.RecentCall
import com.blackcall.ui.lists.loadRecentCalls
import com.blackcall.ui.theme.Coral
import com.blackcall.ui.theme.LocalReducedMotion
import com.blackcall.ui.theme.TealBright
import com.blackcall.ui.theme.TextPrimary
import com.blackcall.ui.theme.TextSecondary

private enum class RecentTab(val title: String) {
    BLOCKED("Blocked"),
    CALL_LOG("Call log")
}

@Composable
fun HomeScreen(
    permissions: PermissionController,
    contentPadding: PaddingValues,
    onSeeAllActivity: () -> Unit,
    onOpenProfile: () -> Unit,
    vm: HomeViewModel = appViewModel { HomeViewModel(it.repository) }
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val s = ui.settings
    val pending = pendingPermission(permissions, s)
    val reduced = LocalReducedMotion.current

    val context = LocalContext.current
    var recentTab by rememberSaveable { mutableStateOf(RecentTab.BLOCKED) }
    var hasCallLog by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val callLogPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCallLog = it }
    var deviceCalls by remember { mutableStateOf<List<RecentCall>?>(null) }

    LaunchedEffect(recentTab, hasCallLog) {
        if (recentTab == RecentTab.CALL_LOG && hasCallLog) {
            deviceCalls = loadRecentCalls(context).take(100)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        GradientText("Black Call", MaterialTheme.typography.headlineLarge)
                        Text(
                            if (ui.profile.name.isBlank()) "Your calls, on your terms."
                            else "Hi, ${ui.profile.name.trim().substringBefore(' ')}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Avatar(ui.profile.initials(), 44.dp, onClick = onOpenProfile)
                }
            }
        }

        if (pending != null) {
            item(key = "permission-banner") {
                PermissionBanner(pending, permissions, Modifier.animateItem())
            }
        }

        item {
            Staggered(1) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ShieldHero(active = s.protectionEnabled)
                    AnimatedContent(
                        targetState = s.protectionEnabled,
                        transitionSpec = {
                            if (reduced) EnterTransition.None togetherWith ExitTransition.None
                            else (fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 }) togetherWith fadeOut(
                                tween(150)
                            )
                        },
                        label = "statusText"
                    ) { on ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (on) "You're protected" else "Protection is paused",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                statusLine(on, ui.today),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    AnimatedContent(
                        targetState = s.protectionEnabled,
                        transitionSpec = {
                            if (reduced) EnterTransition.None togetherWith ExitTransition.None
                            else fadeIn(tween(300)) togetherWith fadeOut(tween(150))
                        },
                        label = "toggleButton"
                    ) { on ->
                        if (on) {
                            GhostButton(
                                "Pause protection",
                                { vm.update { it.copy(protectionEnabled = false) } })
                        } else {
                            GradientButton(
                                "Turn protection on",
                                { vm.update { it.copy(protectionEnabled = true) } })
                        }
                    }
                }
            }
        }

        item {
            Staggered(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Today", ui.today, Modifier.weight(1f))
                    StatTile("7 days", ui.week, Modifier.weight(1f))
                    StatTile("All time", ui.total, Modifier.weight(1f))
                }
            }
        }

        item {
            Staggered(3) {
                GlassCard(Modifier.fillMaxWidth()) {
                    SectionHeader("Last 7 days")
                    Spacer(Modifier.height(8.dp))
                    WeekBarChart(ui.last7, ui.dayLabels)
                }
            }
        }

        item {
            Staggered(4) {
                GlassCard(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    SettingSwitchRow(
                        icon = Icons.Rounded.Report,
                        title = "Spam shield",
                        subtitle = "Block calls that look like spam or scams",
                        checked = s.blockSpam,
                        onCheckedChange = { v -> vm.update { it.copy(blockSpam = v) } }
                    )
                    SettingSwitchRow(
                        icon = Icons.Rounded.PersonOff,
                        title = "Unknown callers",
                        subtitle = "Block numbers that aren't in your contacts",
                        checked = s.blockUnknown,
                        onCheckedChange = { v -> vm.update { it.copy(blockUnknown = v) } }
                    )
                    SettingSwitchRow(
                        icon = Icons.Rounded.Contacts,
                        title = "Contacts only",
                        subtitle = "Only contacts and your allowed list can ring",
                        checked = s.contactsOnly,
                        onCheckedChange = { v -> vm.update { it.copy(contactsOnly = v) } }
                    )
                }
            }
        }


            item {
                Staggered(5) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Recent activity") {
                            val showSeeAll = recentTab == RecentTab.BLOCKED
                            TextButton(
                                onClick = onSeeAllActivity,
                                enabled = showSeeAll,
                                modifier = Modifier.alpha(if (showSeeAll) 1f else 0f)
                            ) {
                                Text("See all", color = TealBright)
                            }
                        }
                        SegmentedControl(
                            options = RecentTab.entries,
                            selected = recentTab,
                            label = { it.title },
                            onSelect = { recentTab = it }
                        )
                    }
                }
            }

        when (recentTab) {
            RecentTab.BLOCKED -> {
                if (ui.recent.isEmpty()) {
                    item {
                        EmptyState(
                            icon = R.drawable.shield_svgrepo_com,
                            title = "All quiet",
                            subtitle = "Blocked calls will show up here."
                        )
                    }
                } else {
                    items(ui.recent, key = { "b-" + it.id }) { entry ->
                        CallLogRow(entry, Modifier.animateItem())
                    }
                }
            }

            RecentTab.CALL_LOG -> {
                val calls = deviceCalls
                when {
                    !hasCallLog -> item {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Text(
                                "Allow call log access",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "See your phone's recent calls here. This stays on your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            GradientButton(
                                text = "Allow",
                                onClick = { callLogPermission.launch(Manifest.permission.READ_CALL_LOG) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    calls == null -> item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = TealBright,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    calls.isEmpty() -> item {
                        EmptyState(
                            icon = R.drawable.origin_svgrepo_com,
                            title = "No recent calls",
                            subtitle = "Your call history is empty."
                        )
                    }

                    else -> items(calls, key = { "c-" + it.number }) { call ->
                        CallRow(call)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    GlassCard(modifier, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp)) {
        AnimatedCounter(value, MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

@Composable
private fun PermissionBanner(
    status: PermissionStatus,
    permissions: PermissionController,
    modifier: Modifier = Modifier
) {
    val required = status.permission.importance == Importance.REQUIRED
    GlassCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Warning,
                null,
                tint = if (required) Coral else TealBright,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (required) "Finish setup: ${status.permission.title}" else "${status.permission.title} access needed",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (required) "Black Call can't block calls until this is allowed. ${status.permission.why}"
            else "Your filters rely on knowing who your contacts are. Without it, people you know could be blocked.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        GradientButton(
            text = if (permissions.needsSettings(status.permission)) "Open settings" else "Allow",
            onClick = { permissions.request(status.permission) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** The most important thing the user still has to fix, or null when everything is in order. */
private fun pendingPermission(p: PermissionController, s: FilterSettings): PermissionStatus? {
    val all = p.statuses
    all.firstOrNull {
        it.permission.importance == Importance.REQUIRED &&
                it.state != PermState.GRANTED && it.state != PermState.UNAVAILABLE
    }?.let { return it }
    val contacts = all.first { it.permission == AppPermission.CONTACTS }
    return if (!contacts.granted && (s.blockUnknown || s.contactsOnly)) contacts else null
}

private fun statusLine(on: Boolean, today: Int): String = when {
    !on -> "Every call will ring until you turn protection back on."
    today == 0 -> "Nothing blocked today. We're watching quietly."
    today == 1 -> "1 unwanted call stopped today."
    else -> "$today unwanted calls stopped today."
}