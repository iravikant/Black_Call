package com.blackcall

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blackcall.domain.Appearance
import com.blackcall.permissions.rememberPermissionController
import com.blackcall.ui.navigation.MainScaffold
import com.blackcall.ui.onboarding.OnboardingScreen
import com.blackcall.ui.theme.BlackCallTheme
import com.blackcall.ui.theme.LocalReducedMotion
import com.blackcall.ui.theme.resolveDark
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the splash up until saved settings are read, so there's no wrong-theme or onboarding flash.
        installSplashScreen().setKeepOnScreenCondition { !ready }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent { AppRoot(onReady = { ready = true }) }
    }
}

@Composable
private fun AppRoot(onReady: () -> Unit) {
    val activity = LocalContext.current as ComponentActivity
    val store = activity.container.settingsStore
    val appearanceState by remember { store.appearance }.collectAsStateWithLifecycle(initialValue = null)
    val flags by remember { store.flags }.collectAsStateWithLifecycle(initialValue = null)

    val appearance = appearanceState ?: Appearance()
    val systemDark = isSystemInDarkTheme()
    val dark = resolveDark(appearance, systemDark)

    // Status/navigation bar icons follow the chosen theme, not just the system's.
    DisposableEffect(dark) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
        )
        onDispose { }
    }

    LaunchedEffect(appearanceState, flags) { if (appearanceState != null && flags != null) onReady() }

    BlackCallTheme(appearance, systemDark) {
        val permissions = rememberPermissionController()
        val scope = rememberCoroutineScope()
        val reduced = LocalReducedMotion.current
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val done = flags?.onboardingDone
            if (done != null && appearanceState != null) {
                AnimatedContent(
                    targetState = done,
                    transitionSpec = {
                        if (reduced) EnterTransition.None togetherWith ExitTransition.None
                        else fadeIn(tween(500)) togetherWith fadeOut(tween(300))
                    },
                    label = "root"
                ) { finished ->
                    if (finished) MainScaffold(permissions)
                    else OnboardingScreen(permissions, onFinish = { scope.launch { store.setOnboardingDone(true) } })
                }
            }
        }
    }
}
