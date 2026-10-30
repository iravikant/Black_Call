package com.blackcall

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blackcall.permissions.rememberPermissionController
import com.blackcall.ui.navigation.MainScaffold
import com.blackcall.ui.onboarding.OnboardingScreen
import com.blackcall.ui.theme.BlackCallTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the splash up until saved settings are read, so there's no onboarding flash.
        installSplashScreen().setKeepOnScreenCondition { !ready }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            BlackCallTheme { BlackCallRoot(onReady = { ready = true }) }
        }
    }
}

@Composable
private fun BlackCallRoot(onReady: () -> Unit) {
    val store = LocalContext.current.container.settingsStore
    val flags by remember { store.flags }.collectAsStateWithLifecycle(initialValue = null)
    val permissions = rememberPermissionController()
    val scope = rememberCoroutineScope()

    LaunchedEffect(flags) { if (flags != null) onReady() }

    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
        val done = flags?.onboardingDone
        if (done != null) {
            AnimatedContent(
                targetState = done,
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
                label = "root"
            ) { finished ->
                if (finished) MainScaffold(permissions)
                else OnboardingScreen(permissions, onFinish = { scope.launch { store.setOnboardingDone(true) } })
            }
        }
    }
}
