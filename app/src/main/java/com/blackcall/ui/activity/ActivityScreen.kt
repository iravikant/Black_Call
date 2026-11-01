package com.blackcall.ui.activity

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blackcall.R
import com.blackcall.data.db.CallLogEntry
import com.blackcall.ui.Format
import com.blackcall.ui.LocalSnackbar
import com.blackcall.ui.appViewModel
import com.blackcall.ui.components.ConfirmDialog
import com.blackcall.ui.components.DestructiveColor
import com.blackcall.ui.components.EmptyState
import com.blackcall.ui.components.GhostButton
import com.blackcall.ui.components.GradientButton
import com.blackcall.ui.components.GradientText
import com.blackcall.ui.components.Staggered
import com.blackcall.ui.theme.Black
import com.blackcall.ui.theme.BrandGradient
import com.blackcall.ui.theme.SubtleMid
import com.blackcall.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ActivityScreen(
    contentPadding: PaddingValues,
    vm: ActivityViewModel = appViewModel { ActivityViewModel(it.repository) }
) {
    val groups by vm.groups.collectAsStateWithLifecycle()
    val total by vm.totalCount.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    fun say(msg: String) = scope.launch {
        snackbar.currentSnackbarData?.dismiss()
        snackbar.showSnackbar(msg)
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Staggered(0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        GradientText("Activity", MaterialTheme.typography.headlineLarge)
                        Text(
                            if (total == 0) "Every blocked call is recorded here."
                            else "$total blocked ${if (total == 1) "call" else "calls"} recorded on this phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    if (total > 0) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Rounded.DeleteSweep, "Clear activity", tint = TextSecondary)
                        }
                    }
                }
            }
        }

        item {
            Staggered(1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LogFilter.entries) { f ->
                        FilterPill(f.title, selected = f == filter) { vm.setFilter(f) }
                    }
                }
            }
        }

        if (groups.isEmpty()) {
            item {
                EmptyState(
                    R.drawable.origin_svgrepo_com,
                    if (total == 0) "Nothing blocked yet" else "No matches",
                    if (total == 0) "When Black Call stops a call, you'll see exactly why."
                    else "Try another filter."
                )
            }
        }

        val grouped = groups.groupBy { Format.day(it.latest.timestamp) }
        grouped.forEach { (day, rows) ->
            item(key = "day-$day") {
                Text(
                    day.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp).animateItem()
                )
            }
            items(rows, key = { it.latest.id }) { group ->
                val entry = group.latest
                val expanded = expandedId == entry.id
                CallLogRow(
                    entry = entry,
                    count = group.count,
                    modifier = Modifier.animateItem(),
                    expanded = expanded,
                    onClick = { expandedId = if (expanded) null else entry.id }
                ) {
                    EntryActions(
                        entry = entry,
                        onBlock = { vm.block(entry); say("Number added to your blocked list") },
                        onAllow = { vm.allow(entry); say("Number added to your allowed list") },
                        onDelete = { vm.deleteGroup(group); expandedId = null }
                    )
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear activity?",
            message = "This removes the call history from this phone. Your blocked and allowed lists are not affected.",
            confirmText = "Clear",
            destructive = true,
            onConfirm = { vm.clearAll(); expandedId = null },
            onDismiss = { confirmClear = false }
        )
    }
}

@Composable
private fun EntryActions(
    entry: CallLogEntry,
    onBlock: () -> Unit,
    onAllow: () -> Unit,
    onDelete: () -> Unit
) {
    val hasNumber = entry.normalized.isNotEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (hasNumber) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradientButton("Always block", onBlock, Modifier.weight(1f), icon = Icons.Rounded.Block)
                GhostButton("Mark as safe", onAllow, Modifier.weight(1f))
            }
        }
        GhostButton("Delete entry", onDelete, Modifier.fillMaxWidth(), color = DestructiveColor)
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val textColor by animateColorAsState(if (selected) Black else TextSecondary, label = "pillText")
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) BrandGradient else SolidColor(SubtleMid))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = textColor, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(0.dp))
    }
}