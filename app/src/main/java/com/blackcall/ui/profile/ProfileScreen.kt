package com.blackcall.ui.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blackcall.container
import com.blackcall.domain.AccentId
import com.blackcall.domain.Profile
import com.blackcall.domain.ThemeMode
import com.blackcall.ui.appViewModel
import com.blackcall.ui.components.AnimatedCounter
import com.blackcall.ui.components.Avatar
import com.blackcall.ui.components.BlackCallTextField
import com.blackcall.ui.components.GhostButton
import com.blackcall.ui.components.GlassCard
import com.blackcall.ui.components.GradientText
import com.blackcall.ui.components.SectionHeader
import com.blackcall.ui.components.SettingSwitchRow
import com.blackcall.ui.components.Staggered
import com.blackcall.ui.theme.Black
import com.blackcall.ui.theme.BrandGradient
import com.blackcall.ui.theme.Hairline
import com.blackcall.ui.theme.Surface1
import com.blackcall.ui.theme.TealBright
import com.blackcall.ui.theme.TextPrimary
import com.blackcall.ui.theme.TextSecondary
import com.blackcall.ui.theme.motion
import com.blackcall.ui.theme.paletteFor
import com.blackcall.ui.theme.resolveDark

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    vm: ProfileViewModel = run {
        val installTime = LocalContext.current.container.installTime
        appViewModel { ProfileViewModel(it.repository, installTime) }
    }
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val a = ui.appearance
    val isDarkNow = resolveDark(a, isSystemInDarkTheme())
    var editName by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      /*  item {
            Staggered(0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TextPrimary)
                    }
                    GradientText("Profile", MaterialTheme.typography.headlineLarge, Modifier.padding(start = 4.dp))
                }
            }
        }*/

        item {
            Staggered(1) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Avatar(ui.profile.initials(), 88.dp, onClick = { editName = true }, contentDescription = "Edit name")
                        Spacer(Modifier.height(14.dp))
                        Text(
                            ui.profile.name.ifBlank { "Add your name" },
                            style = MaterialTheme.typography.titleLarge,
                            color = if (ui.profile.name.isBlank()) TextSecondary else TextPrimary
                        )
                        Text(
                            "Protecting your calls for ${ui.daysProtected} ${if (ui.daysProtected == 1) "day" else "days"}",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        GhostButton("Edit name", { editName = true })
                    }
                }
            }
        }

        item {
            Staggered(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat("Calls blocked", ui.blockedCalls, Modifier.weight(1f))
                    Stat("Blocked", ui.blockedNumbers, Modifier.weight(1f))
                    Stat("Allowed", ui.allowedNumbers, Modifier.weight(1f))
                }
            }
        }

        item {
            Staggered(3) {
                GlassCard(Modifier.fillMaxWidth()) {
                    SectionHeader("Appearance")
                    Text("Theme", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThemeMode.entries.forEach { m ->
                            ThemeTile(m, selected = a.mode == m, Modifier.weight(1f)) {
                                vm.updateAppearance { it.copy(mode = m) }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SettingSwitchRow(
                        icon = Icons.Rounded.DarkMode,
                        title = "Pure black",
                        subtitle = if (isDarkNow) "True black backgrounds, easier on OLED battery" else "Applies in dark mode",
                        checked = a.amoled,
                        onCheckedChange = { v -> vm.updateAppearance { it.copy(amoled = v) } },
                        enabled = isDarkNow
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Accent colour", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AccentId.entries.forEach { id ->
                            AccentSwatch(id, selected = a.accent == id) { vm.updateAppearance { it.copy(accent = id) } }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SettingSwitchRow(
                        icon = Icons.Rounded.Speed,
                        title = "Reduce motion",
                        subtitle = "Turns off decorative animation. Also follows your system setting.",
                        checked = a.reduceMotion,
                        onCheckedChange = { v -> vm.updateAppearance { it.copy(reduceMotion = v) } }
                    )
                }
            }
        }
    }

    if (editName) {
        NameDialog(
            initial = ui.profile.name,
            onSave = { vm.setName(it) },
            onDismiss = { editName = false }
        )
    }
}

@Composable
private fun Stat(label: String, value: Int, modifier: Modifier) {
    GlassCard(modifier, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp)) {
        AnimatedCounter(value, MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

// ---- Theme preview tiles ---------------------------------------------------------------------

private val LightBg = Color(0xFFF1FAF8)
private val LightCard = Color(0xFFDCEEEA)
private val DarkBg = Color(0xFF031614)
private val DarkCard = Color(0xFF14292A)

@Composable
private fun ThemeTile(mode: ThemeMode, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val border by animateColorAsState(if (selected) TealBright else Hairline, motion(tween(180)), label = "tileBorder")
    val brand = BrandGradient
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .clip(shape)
                .border(2.dp, border, shape)
                .clickable(role = Role.RadioButton, onClick = onClick)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                when (mode) {
                    ThemeMode.LIGHT -> miniUi(light = true, brand = brand, from = 0f, to = size.width)
                    ThemeMode.DARK -> miniUi(light = false, brand = brand, from = 0f, to = size.width)
                    ThemeMode.SYSTEM -> {
                        miniUi(light = true, brand = brand, from = 0f, to = size.width / 2f)
                        miniUi(light = false, brand = brand, from = size.width / 2f, to = size.width)
                    }
                }
            }
            CheckBadge(selected, Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            mode.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) TextPrimary else TextSecondary
        )
    }
}

private fun DrawScope.miniUi(light: Boolean, brand: Brush, from: Float, to: Float) {
    clipRect(left = from, right = to) {
        val w = size.width
        val h = size.height
        drawRect(if (light) LightBg else DarkBg)
        drawCircle(brush = brand, radius = w * 0.17f, center = Offset(w * 0.5f, h * 0.30f))
        val card = if (light) LightCard else DarkCard
        val r = CornerRadius(10f, 10f)
        drawRoundRect(card, Offset(w * 0.14f, h * 0.55f), Size(w * 0.72f, h * 0.13f), r)
        drawRoundRect(card, Offset(w * 0.14f, h * 0.73f), Size(w * 0.72f, h * 0.13f), r)
    }
}

@Composable
private fun CheckBadge(visible: Boolean, modifier: Modifier = Modifier) {
    val a by animateFloatAsState(if (visible) 1f else 0f, motion(tween(180)), label = "check")
    Box(
        modifier
            .size(22.dp)
            .graphicsLayer { alpha = a; scaleX = 0.8f + 0.2f * a; scaleY = 0.8f + 0.2f * a }
            .clip(CircleShape)
            .background(BrandGradient),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Check, null, tint = Black, modifier = Modifier.size(14.dp))
    }
}

// ---- Accent swatches -------------------------------------------------------------------------

@Composable
private fun AccentSwatch(id: AccentId, selected: Boolean, onClick: () -> Unit) {
    val p = paletteFor(id)
    val ring by animateColorAsState(if (selected) TealBright else Color.Transparent, motion(tween(180)), label = "ring")
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.RadioButton, onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(58.dp).border(2.dp, ring, CircleShape).padding(5.dp)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(p.start, p.end))),
                contentAlignment = Alignment.Center
            ) { SwatchCheck(selected) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            id.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) TextPrimary else TextSecondary
        )
    }
}

@Composable
private fun SwatchCheck(visible: Boolean) {
    val a by animateFloatAsState(if (visible) 1f else 0f, motion(tween(180)), label = "swatchCheck")
    Icon(
        Icons.Rounded.Check, null, tint = Black,
        modifier = Modifier.size(22.dp).graphicsLayer { alpha = a }
    )
}

// ---- Name dialog -----------------------------------------------------------------------------

@Composable
private fun NameDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        titleContentColor = TextPrimary,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Your name") },
        text = {
            BlackCallTextField(
                value = text,
                onValueChange = { text = it.take(Profile.MAX_NAME) },
                label = "Name",
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text); onDismiss() }) { Text("Save", color = TealBright) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}
