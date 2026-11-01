package com.blackcall.ui.lists

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.ContactsContract
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.blackcall.R
import com.blackcall.ui.components.BlackCallTextField
import com.blackcall.ui.components.EmptyState
import com.blackcall.ui.components.GhostButton
import com.blackcall.ui.components.GradientButton
import com.blackcall.ui.components.SearchField
import com.blackcall.ui.components.SegmentedControl
import com.blackcall.ui.theme.Coral
import com.blackcall.ui.theme.Green
import com.blackcall.ui.theme.Hairline
import com.blackcall.ui.theme.Surface1
import com.blackcall.ui.theme.Teal
import com.blackcall.ui.theme.TealBright
import com.blackcall.ui.theme.TextPrimary
import com.blackcall.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_WITH_PREFIX = 14   // starts with 0 or +  (the '+' is not counted)
private const val MAX_PLAIN = 11         // anything else
private const val MAX_PREFIX_RULE = 6

/**
 * Keeps an optional leading '+' and digits only.
 * Prefix rules: max 6 digits.
 * Numbers: max 14 digits if they start with '+' or '0', otherwise max 11.
 */
private fun sanitizeNumber(input: String, tab: ListTab): String {
    val hasPlus = input.trimStart().startsWith("+")
    val allDigits = input.filter { it.isDigit() }
    val max = when {
        tab == ListTab.PREFIXES -> MAX_PREFIX_RULE
        hasPlus || allDigits.startsWith("0") -> MAX_WITH_PREFIX
        else -> MAX_PLAIN
    }
    val digits = allDigits.take(max)
    return if (hasPlus) "+$digits" else digits
}

// ---------------------------------------------------------------------------------------------
// Call log helpers (shared with HomeScreen)
// ---------------------------------------------------------------------------------------------

internal data class RecentCall(
    val number: String,
    val name: String,
    val date: Long,
    val type: Int,
    val count: Int = 1
)

internal suspend fun loadRecentCalls(context: Context): List<RecentCall> =
    withContext(Dispatchers.IO) {
        val result = LinkedHashMap<String, RecentCall>()
        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.DATE,
                    CallLog.Calls.TYPE
                ),
                null, null, "${CallLog.Calls.DATE} DESC"
            )?.use { c ->
                var scanned = 0
                while (c.moveToNext() && scanned < 300) {
                    scanned++
                    val number = c.getString(0).orEmpty()
                    if (number.isBlank() || number.startsWith("-")) continue   // private / unknown
                    val key = number.filter { it.isDigit() }
                    if (key.isEmpty()) continue

                    val existing = result[key]
                    if (existing != null) {
                        result[key] = existing.copy(count = existing.count + 1)
                        continue
                    }
                    if (result.size >= 50) continue
                    result[key] = RecentCall(
                        number = number,
                        name = c.getString(1).orEmpty(),
                        date = c.getLong(2),
                        type = c.getInt(3)
                    )
                }
            }
        } catch (_: SecurityException) {
        }
        result.values.toList()
    }

internal data class TypeMeta(val icon: ImageVector, val label: String, val color: Color)

@Composable
internal fun typeMeta(type: Int): TypeMeta = when (type) {
    CallLog.Calls.OUTGOING_TYPE -> TypeMeta(Icons.AutoMirrored.Rounded.CallMade, "Outgoing", TealBright)
    CallLog.Calls.MISSED_TYPE -> TypeMeta(Icons.AutoMirrored.Rounded.CallMissed, "Missed", Coral)
    CallLog.Calls.REJECTED_TYPE -> TypeMeta(Icons.Rounded.Block, "Declined", Coral)
    CallLog.Calls.BLOCKED_TYPE -> TypeMeta(Icons.Rounded.Block, "Blocked", Coral)
    else -> TypeMeta(Icons.AutoMirrored.Rounded.CallReceived, "Incoming", TealBright)
}

internal fun callTime(date: Long): String {
    val now = System.currentTimeMillis()
    return if (DateUtils.isToday(date)) {
        DateUtils.getRelativeTimeSpanString(date, now, DateUtils.MINUTE_IN_MILLIS).toString()
    } else {
        DateUtils.getRelativeTimeSpanString(
            date, now, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
}

internal fun initialsOf(name: String): String =
    name.trim().split(' ').filter { it.isNotBlank() }.take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

@Composable
internal fun CallRow(call: RecentCall, onClick: (() -> Unit)? = null) {
    val meta = typeMeta(call.type)
    val initials = initialsOf(call.name)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Teal.copy(alpha = 0.30f), Green.copy(alpha = 0.16f)))),
            contentAlignment = Alignment.Center
        ) {
            if (initials.isNotEmpty()) {
                Text(initials, style = MaterialTheme.typography.titleSmall, color = TealBright)
            } else {
                Icon(Icons.Rounded.Person, null, tint = TealBright, modifier = Modifier.size(22.dp))
            }
        }

        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                call.name.ifBlank { call.number },
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(meta.icon, null, tint = meta.color, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    buildString {
                        append(meta.label)
                        if (call.count > 1) append(" (${call.count})")
                        append(" · ")
                        append(callTime(call.date))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (call.name.isNotBlank()) {
                Text(
                    call.number,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Call log picker dialog
// ---------------------------------------------------------------------------------------------

@Composable
private fun CallLogPickerDialog(onPick: (RecentCall) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var calls by remember { mutableStateOf<List<RecentCall>?>(null) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { calls = loadRecentCalls(context) }

    val shape = RoundedCornerShape(28.dp)
    val filtered = remember(calls, query) {
        val q = query.trim().lowercase()
        val digits = q.filter { it.isDigit() }
        calls.orEmpty().filter {
            q.isEmpty() || it.name.lowercase().contains(q) ||
                    (digits.isNotEmpty() && it.number.filter { ch -> ch.isDigit() }.contains(digits))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .heightIn(max = 600.dp)
                .clip(shape)
                .background(Surface1)
                .border(1.dp, Hairline, shape)
                .padding(top = 20.dp, bottom = 12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Recent calls", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(
                        "Tap a number to fill it in",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, "Close", tint = TextSecondary)
                }
            }

            val list = calls
            if (list != null && list.size > 5) {
                Spacer(Modifier.height(12.dp))
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search name or number",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    leading = { Icon(Icons.Rounded.Search, null) }
                )
            }
            Spacer(Modifier.height(8.dp))

            when {
                list == null -> Box(
                    Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TealBright, strokeWidth = 3.dp, modifier = Modifier.size(28.dp)
                    )
                }

                filtered.isEmpty() -> EmptyState(
                    R.drawable.origin_svgrepo_com,
                    if (list.isEmpty()) "No recent calls" else "No matches",
                    if (list.isEmpty()) "Your call history is empty." else "Try a different name or number."
                )

                else -> LazyColumn(
                    Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filtered, key = { it.number }) { call ->
                        CallRow(call, onClick = { onPick(call) })
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Add entry sheet
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(
    initialTab: ListTab,
    onSubmit: suspend (ListTab, String, String) -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf(initialTab) }
    var value by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showCallLog by remember { mutableStateOf(false) }

    val contactPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            null, null, null
        )?.use { c ->
            if (c.moveToFirst()) {
                value = sanitizeNumber(c.getString(0).orEmpty(), target)
                if (label.isBlank()) label = c.getString(1).orEmpty().take(40)
                error = null
            }
        }
    }

    val callLogPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showCallLog = true
        else error = "Call log permission is needed to pick from recent calls"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1,
        contentColor = TextPrimary
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text("Add to list", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                when (target) {
                    ListTab.BLOCKED -> "Calls from this number will always be stopped."
                    ListTab.ALLOWED -> "Calls from this number will always ring, even in strict modes."
                    ListTab.PREFIXES -> "Block every number starting with these digits. Use + for country codes, like +92."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(16.dp))
            SegmentedControl(
                options = ListTab.entries,
                selected = target,
                label = { it.title },
                onSelect = {
                    target = it
                    value = sanitizeNumber(value, it)
                    error = null
                }
            )
            Spacer(Modifier.height(16.dp))
            BlackCallTextField(
                value = value,
                onValueChange = {
                    value = sanitizeNumber(it, target)
                    error = null
                },
                label = if (target == ListTab.PREFIXES) "Prefix (e.g. 140 or +92)" else "Phone number",
                keyboardType = KeyboardType.Phone,
                error = error,
                modifier = Modifier.fillMaxWidth()
            )
            if (target != ListTab.PREFIXES) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton(
                        text = "Contacts",
                        onClick = {
                            contactPicker.launch(
                                Intent(
                                    Intent.ACTION_PICK,
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GhostButton(
                        text = "Call log",
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.READ_CALL_LOG
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) showCallLog = true
                            else callLogPermission.launch(Manifest.permission.READ_CALL_LOG)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            BlackCallTextField(
                value = label,
                onValueChange = { label = it.take(40) },
                label = "Note (optional)",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = "Save",
                icon = Icons.Rounded.Add,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        if (onSubmit(target, value, label)) {
                            sheetState.hide()
                            onDismiss()
                        } else {
                            error = if (target == ListTab.PREFIXES) "Enter 1–6 digits, optionally starting with +"
                            else "Enter a valid number: up to 11 digits, or 14 if it starts with 0 or +"
                        }
                    }
                }
            )
        }
    }

    if (showCallLog) {
        CallLogPickerDialog(
            onPick = { call ->
                value = sanitizeNumber(call.number, target)
                if (label.isBlank() && call.name.isNotBlank()) label = call.name.take(40)
                error = null
                showCallLog = false
            },
            onDismiss = { showCallLog = false }
        )
    }
}