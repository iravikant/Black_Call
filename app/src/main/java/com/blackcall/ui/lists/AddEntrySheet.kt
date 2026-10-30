package com.blackcall.ui.lists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.blackcall.ui.components.BlackCallTextField
import com.blackcall.ui.components.GradientButton
import com.blackcall.ui.components.SegmentedControl
import com.blackcall.ui.theme.Surface1
import com.blackcall.ui.theme.TextPrimary
import com.blackcall.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(
    initialTab: ListTab,
    onSubmit: suspend (ListTab, String, String) -> Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf(initialTab) }
    var value by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

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
                onSelect = { target = it; error = null }
            )
            Spacer(Modifier.height(16.dp))
            BlackCallTextField(
                value = value,
                onValueChange = { value = it; error = null },
                label = if (target == ListTab.PREFIXES) "Prefix (e.g. 140 or +92)" else "Phone number",
                keyboardType = KeyboardType.Phone,
                error = error,
                modifier = Modifier.fillMaxWidth()
            )
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
                            else "Enter a valid phone number"
                        }
                    }
                }
            )
        }
    }
}
