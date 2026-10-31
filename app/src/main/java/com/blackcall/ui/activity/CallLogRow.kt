package com.blackcall.ui.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blackcall.data.db.CallLogEntry
import com.blackcall.data.db.signalList
import com.blackcall.domain.BlockAction
import com.blackcall.domain.PhoneNumbers
import com.blackcall.ui.Format
import com.blackcall.ui.components.GlassCard
import com.blackcall.ui.components.IconBadge
import com.blackcall.ui.components.Pill
import com.blackcall.ui.icon
import com.blackcall.ui.theme.BrandGradient
import com.blackcall.ui.theme.GreenBright
import com.blackcall.ui.theme.Hairline
import com.blackcall.ui.theme.TextPrimary
import com.blackcall.ui.theme.TextSecondary
import com.blackcall.ui.theme.motion

/** One blocked call. Collapsed by default; when [expanded] it shows why, and any [actions]. */
@Composable
fun CallLogRow(
    entry: CallLogEntry,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onClick: (() -> Unit)? = null,
    actions: (@Composable ColumnScope.() -> Unit)? = null
) {
    GlassCard(
        modifier = modifier.animateContentSize(tween(280, easing = FastOutSlowInEasing)),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(entry.reason.icon())
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    PhoneNumbers.display(entry.number),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    "${entry.reason.label} · ${Format.time(entry.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Pill(if (entry.action == BlockAction.SILENCE) "Silenced" else "Blocked", color = GreenBright)
        }

        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = 12.dp)) {
                Text(entry.reason.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                if (entry.score > 0) {
                    Spacer(Modifier.height(10.dp))
                    ScoreBar(entry.score)
                }
                entry.signalList().forEach {
                    Text(
                        "•  $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                actions?.let {
                    Spacer(Modifier.height(12.dp))
                    it()
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(score: Int) {
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(score) { target = score / 100f }
    val fraction by animateFloatAsState(target, motion(tween(700, easing = FastOutSlowInEasing)), label = "score")
    Column {
        Text("Spam score  $score / 100", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Hairline)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BrandGradient)
            )
        }
    }
}
