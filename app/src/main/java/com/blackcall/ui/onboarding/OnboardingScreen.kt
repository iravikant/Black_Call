package com.blackcall.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blackcall.permissions.AppPermission
import com.blackcall.permissions.Importance
import com.blackcall.permissions.PermState
import com.blackcall.permissions.PermissionController
import com.blackcall.ui.components.GradientButton
import com.blackcall.ui.components.GradientScreen
import com.blackcall.ui.components.GradientText
import com.blackcall.ui.components.ShieldHero
import com.blackcall.ui.components.Staggered
import com.blackcall.ui.theme.Black
import com.blackcall.ui.theme.BrandGradient
import com.blackcall.ui.theme.TealBright
import com.blackcall.ui.theme.TextPrimary
import com.blackcall.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/** null = the welcome page; otherwise a permission step. */
private val pages: List<AppPermission?> = listOf(null) + AppPermission.entries

@Composable
fun OnboardingScreen(permissions: PermissionController, onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val page = pages[step]
    val state = page?.let { permissions.state(it) }

    fun next() { if (step < pages.lastIndex) step++ else onFinish() }

    // Move on as soon as the permission is granted, or when the device doesn't offer it.
    LaunchedEffect(step, state) {
        if (page != null && state == PermState.GRANTED) { delay(650); next() }
        else if (page != null && state == PermState.UNAVAILABLE) next()
    }

    GradientScreen {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Dots(count = pages.size, current = step)

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        (slideInHorizontally(tween(380)) { it / 3 } + fadeIn(tween(380))) togetherWith
                            (slideOutHorizontally(tween(260)) { -it / 3 } + fadeOut(tween(200)))
                    },
                    label = "onboardingPage"
                ) { i ->
                    val p = pages[i]
                    if (p == null) Welcome() else PermissionPage(p)
                }
            }

            if (page == null) {
                GradientButton("Get started", { next() }, Modifier.fillMaxWidth())
            } else {
                val granted = state == PermState.GRANTED
                GradientButton(
                    text = when {
                        granted -> "Allowed"
                        permissions.needsSettings(page) -> "Open settings"
                        else -> "Allow ${page.title.lowercase()}"
                    },
                    onClick = { permissions.request(page) },
                    enabled = !granted,
                    modifier = Modifier.fillMaxWidth()
                )
                val canSkip = page.importance != Importance.REQUIRED || permissions.needsSettings(page)
                if (canSkip && !granted) {
                    TextButton(onClick = { next() }, Modifier.padding(top = 4.dp)) {
                        Text("Skip for now", color = TextSecondary)
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun Welcome() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ShieldHero(active = true)
        Spacer(Modifier.height(8.dp))
        GradientText("Black Call", MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Stop spam, scams and unknown callers before your phone even rings.",
            style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        listOf(
            "Private by design: nothing leaves your phone",
            "Always lets contacts and emergency calls through",
            "See exactly why each call was blocked"
        ).forEachIndexed { i, line ->
            Staggered(i + 2) {
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, null, tint = TealBright, modifier = Modifier.size(18.dp))
                    Text("  $line", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun PermissionPage(permission: AppPermission) {
    val icon: ImageVector = when (permission) {
        AppPermission.CALL_SCREENING -> Icons.Rounded.Phone
        AppPermission.CONTACTS -> Icons.Rounded.Contacts
        AppPermission.NOTIFICATIONS -> Icons.Rounded.Notifications
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(112.dp).clip(CircleShape).background(BrandGradient),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Black, modifier = Modifier.size(52.dp)) }
        Spacer(Modifier.height(28.dp))
        Text(permission.title, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            when (permission.importance) {
                Importance.REQUIRED -> "REQUIRED"
                Importance.RECOMMENDED -> "RECOMMENDED"
                Importance.OPTIONAL -> "OPTIONAL"
            },
            style = MaterialTheme.typography.labelMedium, color = TealBright
        )
        Spacer(Modifier.height(14.dp))
        Text(
            permission.why, style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary, textAlign = TextAlign.Center
        )
        if (permission == AppPermission.CALL_SCREENING) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Android will ask you to choose Black Call as your call screening app. This doesn't replace your dialer.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Dots(count: Int, current: Int) {
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 26.dp else 8.dp, tween(300), label = "dotW")
            val color by animateColorAsState(
                if (i <= current) TealBright else androidx.compose.ui.graphics.Color(0x33FFFFFF), label = "dotC"
            )
            Box(Modifier.height(8.dp).size(width, 8.dp).clip(RoundedCornerShape(50)).background(color))
        }
    }
}
