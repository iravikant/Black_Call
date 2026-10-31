package com.blackcall.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackcall.ui.theme.Black
import com.blackcall.ui.theme.BrandGradient
import com.blackcall.ui.theme.BrandGradientBright
import com.blackcall.ui.theme.Coral
import com.blackcall.ui.theme.GlassBorderBottom
import com.blackcall.ui.theme.GlassBorderTop
import com.blackcall.ui.theme.GlassBottom
import com.blackcall.ui.theme.GlassTop
import com.blackcall.ui.theme.Green
import com.blackcall.ui.theme.GreenBright
import com.blackcall.ui.theme.Hairline
import com.blackcall.ui.theme.LocalBc
import com.blackcall.ui.theme.LocalReducedMotion
import com.blackcall.ui.theme.ScreenGradient
import com.blackcall.ui.theme.Subtle
import com.blackcall.ui.theme.SubtleMid
import com.blackcall.ui.theme.SubtleStrong
import com.blackcall.ui.theme.Surface2
import com.blackcall.ui.theme.Teal
import com.blackcall.ui.theme.TealBright
import com.blackcall.ui.theme.TealDeep
import com.blackcall.ui.theme.TextPrimary
import com.blackcall.ui.theme.TextSecondary
import com.blackcall.ui.theme.TextTertiary
import com.blackcall.ui.theme.motion
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------------------------
// Backgrounds & motion helpers
// ---------------------------------------------------------------------------------------------

/** Black → teal-lit gradient canvas with two soft ambient glows. */
@Composable
fun GradientScreen(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val glowA = Teal
    val glowB = Green
    Box(
        modifier
            .fillMaxSize()
            .background(ScreenGradient)
            .drawBehind {
                val r = size.width * 0.75f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(glowA.copy(alpha = 0.22f), glowA.copy(alpha = 0f)),
                        center = Offset(size.width * 0.9f, size.height * 0.04f), radius = r
                    ),
                    radius = r, center = Offset(size.width * 0.9f, size.height * 0.04f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(glowB.copy(alpha = 0.10f), glowB.copy(alpha = 0f)),
                        center = Offset(size.width * 0.05f, size.height * 0.85f), radius = r
                    ),
                    radius = r, center = Offset(size.width * 0.05f, size.height * 0.85f)
                )
            },
        content = content
    )
}

/** Springy press feedback. */
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.97f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = motion(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)),
        label = "pressScale"
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Fades and lifts content in, [index] * 70ms after first composition. Doesn't shift layout. */
@Composable
fun Staggered(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            delay(index * 70L)
            progress.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 28.dp.toPx()
        }
    ) { content() }
}

// ---------------------------------------------------------------------------------------------
// Cards, buttons, text
// ---------------------------------------------------------------------------------------------

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val source = remember { MutableInteractionSource() }
    var m = modifier
    if (onClick != null) m = m.pressScale(source)
    m = m
        .clip(shape)
        .background(Brush.verticalGradient(listOf(GlassTop, GlassBottom)))
        .border(1.dp, Brush.verticalGradient(listOf(GlassBorderTop, GlassBorderBottom)), shape)
    if (onClick != null) {
        m = m.clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
    }
    Column(m.padding(contentPadding), content = content)
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val source = remember { MutableInteractionSource() }
    val a by animateFloatAsState(if (enabled) 1f else 0.4f, label = "btnAlpha")
    Row(
        modifier
            .pressScale(source)
            .alpha(a)
            .clip(RoundedCornerShape(16.dp))
            .background(BrandGradient)
            .clickable(
                enabled = enabled, interactionSource = source, indication = null,
                role = Role.Button, onClick = onClick
            )
            .heightIn(min = 52.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Black, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = Black, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
    }
}

/** Outlined, low-emphasis button. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = TealBright
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(source)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GradientText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    Text(text, modifier, style = style.copy(brush = BrandGradientBright))
}

@Composable
fun AnimatedCounter(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary
) {
    // Starts at 0 so numbers count up on first display, then tween between later values.
    var target by remember { mutableIntStateOf(0) }
    LaunchedEffect(value) { target = value }
    val shown by animateIntAsState(target, motion(tween(900, easing = FastOutSlowInEasing)), label = "counter")
    Text(shown.toString(), modifier, style = style, color = color)
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

// ---------------------------------------------------------------------------------------------
// Hero
// ---------------------------------------------------------------------------------------------

/**
 * The home-screen centrepiece: a gradient shield with two expanding pulse rings and a slowly
 * rotating arc while protection is on; it settles to a dim, still shield when paused.
 */
@Composable
fun ShieldHero(active: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "hero")
    val reduced = LocalReducedMotion.current
    val pulseA by infinite.loop(
        reduced, 1f, 0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing)), "pulseA"
    )
    val pulseB by infinite.loop(
        reduced, 1f, 0f, 1f,
        infiniteRepeatable(tween(2800, easing = LinearEasing), initialStartOffset = StartOffset(1400)),
        "pulseB"
    )
    val spin by infinite.loop(
        reduced, 0f, 0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), "spin"
    )
    val bob by infinite.loop(
        reduced, 0f, -5f, 5f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), "bob"
    )
    val on by animateFloatAsState(if (active) 1f else 0f, motion(tween(700)), label = "heroOn")

    // Theme colours are read here: the draw lambda below isn't composable.
    val teal = Teal
    val tealBright = TealBright
    val green = Green
    val ringColor = SubtleMid

    Box(modifier.size(264.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val base = size.minDimension / 2f

            listOf(pulseA, pulseB).forEach { p ->
                drawCircle(
                    color = tealBright.copy(alpha = (1f - p) * 0.38f * on),
                    radius = base * (0.5f + 0.5f * p),
                    center = c,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(teal.copy(alpha = 0.10f + 0.30f * on), teal.copy(alpha = 0f)),
                    center = c, radius = base * 0.95f
                ),
                radius = base * 0.95f, center = c
            )
            val ringR = base * 0.60f
            rotate(spin, pivot = c) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(teal.copy(alpha = 0f), teal, green, green.copy(alpha = 0f)), center = c
                    ),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(c.x - ringR, c.y - ringR),
                    size = Size(ringR * 2, ringR * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    alpha = on
                )
            }
            drawCircle(
                color = ringColor, radius = ringR, center = c,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        Box(
            Modifier
                .size(120.dp)
                .graphicsLayer { translationY = bob * density }
                .clip(CircleShape)
                .background(Surface2)
                .background(BrandGradient, alpha = on),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(if (active) com.blackcall.R.drawable.shield_svgrepo_com else com.blackcall.R.drawable.shield_svgrepo_com__1_),
                contentDescription = if (active) "Protection active" else "Protection paused",
                tint = lerp(TextTertiary, Black, on),
                modifier = Modifier.size(60.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Rows & controls
// ---------------------------------------------------------------------------------------------

/** A looping animation that holds still at [rest] when reduced motion is on. */
@Composable
private fun InfiniteTransition.loop(
    reduced: Boolean, rest: Float, from: Float, to: Float,
    spec: InfiniteRepeatableSpec<Float>, label: String
): State<Float> =
    if (reduced) remember { mutableFloatStateOf(rest) } else animateFloat(from, to, spec, label)

@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = TealBright) {
    Box(
        modifier
            .size(42.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.linearGradient(listOf(Teal.copy(alpha = 0.28f), Green.copy(alpha = 0.14f)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun BlackCallSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true) {
    val dark = LocalBc.current.isDark
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = if (dark) Black else Color.White,
            checkedTrackColor = if (dark) TealBright else Teal,
            checkedBorderColor = if (dark) TealBright else Color.Transparent,
            uncheckedThumbColor = TextSecondary,
            uncheckedTrackColor = if (dark) Surface2 else Subtle,
            uncheckedBorderColor = if (dark) Hairline else SubtleStrong,
            disabledCheckedTrackColor = Teal.copy(alpha = 0.4f),
            disabledUncheckedTrackColor = if (dark) Surface2.copy(alpha = 0.5f) else Subtle
        )
    )
}

@Composable
fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (enabled) TextPrimary else TextTertiary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        BlackCallSwitch(checked, null, enabled)
    }
}

@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Subtle)
            .padding(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val text by animateColorAsState(if (isSelected) Black else TextSecondary, motion(tween(250)), label = "segText")
            val fill by animateFloatAsState(if (isSelected) 1f else 0f, motion(tween(280)), label = "segFill")
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandGradient, alpha = fill)
                    .clickable(role = Role.Tab) { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label(option), color = text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Chart & empty state
// ---------------------------------------------------------------------------------------------

/** Seven bars that grow in one after another. The last bar (today) is highlighted. */
@Composable
fun WeekBarChart(values: List<Int>, labels: List<String>, modifier: Modifier = Modifier) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val reduced = LocalReducedMotion.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        values.forEachIndexed { i, v ->
            var grown by remember { mutableStateOf(false) }
            LaunchedEffect(v) {
                if (!reduced) delay(i * 70L)
                grown = true
            }
            val fraction by animateFloatAsState(
                if (grown) v / max.toFloat() else 0f,
                motion(tween(750, easing = FastOutSlowInEasing)), label = "bar$i"
            )
            val isToday = i == values.lastIndex
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (v > 0) v.toString() else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) TealBright else TextSecondary,
                    modifier = Modifier.height(16.dp)
                )
                Box(Modifier.height(88.dp).width(20.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceAtLeast(0.05f))
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (v == 0) Brush.verticalGradient(listOf(SubtleStrong, SubtleStrong))
                                else if (isToday) Brush.verticalGradient(listOf(GreenBright, TealBright))
                                else Brush.verticalGradient(listOf(Teal.copy(alpha = 0.75f), TealDeep.copy(alpha = 0.6f)))
                            )
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    labels.getOrElse(i) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) TextPrimary else TextTertiary
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "empty")
    val bob by infinite.animateFloat(
        -6f, 6f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "emptyBob"
    )
    Column(
        modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .graphicsLayer { translationY = bob * density }
                .size(88.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Teal.copy(alpha = 0.30f), Teal.copy(alpha = 0.04f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = TealBright,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** Small coloured pill, e.g. the block reason on a log entry. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = TealBright) {
    Text(
        text,
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

val DestructiveColor = Coral

// ---------------------------------------------------------------------------------------------
// Profile avatar
// ---------------------------------------------------------------------------------------------

/** Circular brand-gradient avatar showing [initials], or a person icon when there are none. */
@Composable
fun Avatar(
    initials: String,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentDescription: String = "Profile"
) {
    val source = remember { MutableInteractionSource() }
    var m = modifier.size(size)
    if (onClick != null) m = m.pressScale(source).semantics { this.contentDescription = contentDescription }
    m = m.clip(CircleShape).background(BrandGradient)
    if (onClick != null) {
        m = m.clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
    }
    Box(m, contentAlignment = Alignment.Center) {
        if (initials.isEmpty()) {
            Icon(Icons.Rounded.Person, null, tint = Black, modifier = Modifier.size(size * 0.52f))
        } else {
            Text(
                initials, color = Black,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = (size.value * 0.38f).sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}
