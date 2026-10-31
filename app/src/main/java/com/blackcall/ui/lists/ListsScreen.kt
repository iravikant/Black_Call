package com.blackcall.ui.lists

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blackcall.R
import com.blackcall.data.db.NumberRule
import com.blackcall.data.db.PrefixRule
import com.blackcall.domain.PhoneNumbers
import com.blackcall.ui.Format
import com.blackcall.ui.LocalSnackbar
import com.blackcall.ui.appViewModel
import com.blackcall.ui.components.EmptyState
import com.blackcall.ui.components.GlassCard
import com.blackcall.ui.components.GradientButton
import com.blackcall.ui.components.GradientText
import com.blackcall.ui.components.IconBadge
import com.blackcall.ui.components.SearchField
import com.blackcall.ui.components.SegmentedControl
import com.blackcall.ui.components.Staggered
import com.blackcall.ui.theme.Coral
import com.blackcall.ui.theme.TextPrimary
import com.blackcall.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    contentPadding: PaddingValues,
    vm: ListsViewModel = appViewModel { ListsViewModel(it.repository) }
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }

    fun undoSnack(message: String, onUndo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(
                message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (r == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Staggered(0) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column {
                            GradientText("Lists", MaterialTheme.typography.headlineLarge)
                            Text(
                                "Decide who always gets through, and who never does.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        SegmentedControl(ListTab.entries, tab, { it.title }, vm::selectTab)
                        SearchField(
                            value = query,
                            onValueChange = vm::setQuery,
                            placeholder = "Search",
                            modifier = Modifier.fillMaxWidth(),
                            leading = { Icon(Icons.Rounded.Search, null) }
                        )
                    }
                }
            }

            when (tab) {
                ListTab.BLOCKED -> numberItems(
                    rules = ui.blocked, icon = Icons.Rounded.Block,
                    emptyIcon = Icons.Rounded.Block, emptyTitle = "No blocked numbers",
                    emptySub = "Numbers you block always get stopped. Tap Add to start.",
                    searching = query.isNotBlank(),
                    onRemove = { r ->
                        vm.remove(r)
                        undoSnack("Removed ${PhoneNumbers.display(r.number)}") { vm.restore(r) }
                    }
                )

                ListTab.ALLOWED -> numberItems(
                    rules = ui.allowed, icon = Icons.Rounded.VerifiedUser,
                    emptyIcon = Icons.Rounded.VerifiedUser, emptyTitle = "No allowed numbers",
                    emptySub = "Allowed numbers always ring, even in contacts-only mode.",
                    searching = query.isNotBlank(),
                    onRemove = { r ->
                        vm.remove(r)
                        undoSnack("Removed ${PhoneNumbers.display(r.number)}") { vm.restore(r) }
                    }
                )

                ListTab.PREFIXES -> {
                    if (ui.prefixes.isEmpty()) {
                        item {
                            EmptyState(
                                R.drawable.block_svgrepo_com,
                                if (query.isBlank()) "No prefix rules" else "No matches",
                                "Block whole number ranges, like telemarketing series (140) or a country code (+92)."
                            )
                        }
                    } else {
                        items(ui.prefixes, key = { "p-" + it.prefix }) { p ->
                            SwipeRow(
                                onDelete = {
                                    vm.remove(p)
                                    undoSnack("Removed prefix ${p.prefix}") { vm.restore(p) }
                                },
                                modifier = Modifier.animateItem()
                            ) {
                                RuleRow(
                                    Icons.Rounded.Dialpad,
                                    p.prefix + "…",
                                    p.label.ifBlank { "Prefix rule" },
                                    p.createdAt
                                ) {
                                    vm.remove(p)
                                    undoSnack("Removed prefix ${p.prefix}") { vm.restore(p) }
                                }
                            }
                        }
                    }
                }
            }
        }

        GradientButton(
            text = "Add",
            icon = Icons.Rounded.Add,
            onClick = { showSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp)
        )
    }

    if (showSheet) {
        AddEntrySheet(
            initialTab = tab,
            onSubmit = { target, value, label ->
                vm.add(target, value, label).also { ok -> if (ok) vm.selectTab(target) }
            },
            onDismiss = { showSheet = false }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.numberItems(
    rules: List<NumberRule>,
    icon: ImageVector,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptySub: String,
    searching: Boolean,
    onRemove: (NumberRule) -> Unit
) {
    if (rules.isEmpty()) {
        item {
            EmptyState(
                R.drawable.block_svgrepo_com,
                if (searching) "No matches" else emptyTitle,
                if (searching) "Try a different search." else emptySub
            )
        }
        return
    }
    items(rules, key = { it.normalized }) { r ->
        SwipeRow(onDelete = { onRemove(r) }, modifier = Modifier.animateItem()) {
            RuleRow(
                icon,
                PhoneNumbers.display(r.number),
                r.label.ifBlank { "Added ${Format.day(r.createdAt)}" },
                r.createdAt
            ) {
                onRemove(r)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeRow(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete(); true
            } else false
        }
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Coral.copy(alpha = 0.25f))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Rounded.Delete, null, tint = Coral)
            }
        },
        content = { content() }
    )
}

@Composable
private fun RuleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    @Suppress("UNUSED_PARAMETER") createdAt: Long,
    onDelete: () -> Unit
) {
    GlassCard(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon)
            Column(Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Remove",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
