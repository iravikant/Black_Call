package com.blackcall.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.graphicsLayer
import com.blackcall.permissions.PermissionController
import com.blackcall.ui.LocalSnackbar
import com.blackcall.ui.activity.ActivityScreen
import com.blackcall.ui.components.GradientScreen
import com.blackcall.ui.home.HomeScreen
import com.blackcall.ui.lists.ListsScreen
import com.blackcall.ui.profile.ProfileScreen
import com.blackcall.ui.settings.SettingsScreen
import com.blackcall.ui.theme.BarBackground
import com.blackcall.ui.theme.BarBorderBottom
import com.blackcall.ui.theme.BarBorderTop
import com.blackcall.ui.theme.Black
import com.blackcall.ui.theme.BrandGradient
import com.blackcall.ui.theme.LocalReducedMotion
import com.blackcall.ui.theme.Surface1
import com.blackcall.ui.theme.TealBright
import com.blackcall.ui.theme.TextPrimary
import com.blackcall.ui.theme.TextSecondary
import com.blackcall.ui.theme.motion
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import com.blackcall.R
private const val PROFILE = "profile"


private enum class Dest(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int,
    @DrawableRes val iconSelected: Int = icon      // optional: a different drawable when selected
) {
    HOME("home", "Shield", R.drawable.shield_svgrepo_com),
    LISTS("lists", "Lists", R.drawable.block_svgrepo_com),
    ACTIVITY("activity", "Activity", R.drawable.origin_svgrepo_com),
    SETTINGS("settings", "Settings", R.drawable.setting_3_svgrepo_com),
    PROFILE("profile", "Profile", R.drawable.user_circle_svgrepo_com)
}
/*private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Shield", Icons.Rounded.Shield),
    LISTS("lists", "Lists", Icons.Rounded.Block),
    ACTIVITY("activity", "Activity", Icons.Rounded.History),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings),

    PROFILE("profile", "Profile", Icons.Rounded.Person)

}*/

@Composable
fun MainScaffold(permissions: PermissionController) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination
    val reduced = LocalReducedMotion.current

    fun go(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        GradientScreen {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = {
                    SnackbarHost(snackbar, Modifier.padding(bottom = 4.dp)) { data ->
                        Snackbar(
                            data,
                            shape = RoundedCornerShape(16.dp),
                            containerColor = Surface1,
                            contentColor = TextPrimary,
                            actionColor = TealBright
                        )
                    }
                },
                bottomBar = {
                    FloatingBar(
                        selectedRoute = Dest.entries.firstOrNull { d -> current?.hierarchy?.any { it.route == d.route } == true }?.route,
                        onSelect = ::go
                    )
                }
            ) { inner ->
                NavHost(
                    navController = nav,
                    startDestination = Dest.HOME.route,
                    enterTransition = {
                        if (reduced) EnterTransition.None
                        else fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 24 }
                    },
                    exitTransition = { if (reduced) ExitTransition.None else fadeOut(tween(160)) },
                    popEnterTransition = {
                        if (reduced) EnterTransition.None
                        else fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 24 }
                    },
                    popExitTransition = { if (reduced) ExitTransition.None else fadeOut(tween(160)) }
                ) {
                    composable(Dest.HOME.route) {
                        HomeScreen(
                            permissions, inner,
                            onSeeAllActivity = { go(Dest.ACTIVITY.route) },
                            onOpenProfile = { go(Dest.PROFILE.route) }
                        )
                    }
                    composable(Dest.LISTS.route) { ListsScreen(inner) }
                    composable(Dest.ACTIVITY.route) { ActivityScreen(inner) }
                    composable(Dest.SETTINGS.route) {
                        SettingsScreen(
                            permissions, inner,
                            onOpenProfile = { go(Dest.PROFILE.route) }
                        )
                    }
                    composable(Dest.PROFILE.route) {
                        ProfileScreen(inner, onBack = { go(Dest.HOME.route) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingBar(selectedRoute: String?, onSelect: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(BarBackground)
                .border(1.dp, Brush.verticalGradient(listOf(BarBorderTop, BarBorderBottom)), RoundedCornerShape(50))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Dest.entries.forEach { d -> BarItem(d, d.route == selectedRoute) { onSelect(d.route) } }
        }
    }
}

@Composable
private fun BarItem(dest: Dest, selected: Boolean, onClick: () -> Unit) {
    val fill by animateFloatAsState(if (selected) 1f else 0f, motion(tween(300)), label = "barFill")
    val tint by animateColorAsState(if (selected) Black else TextSecondary, motion(tween(250)), label = "barTint")
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(BrandGradient, alpha = fill)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .graphicsLayer { scaleX = 0.96f + 0.04f * fill; scaleY = 0.96f + 0.04f * fill },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(if (selected) dest.iconSelected else dest.icon),
            contentDescription = dest.label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        AnimatedVisibility(
            visible = selected,
            enter = if (LocalReducedMotion.current) EnterTransition.None else expandHorizontally(tween(280)) + fadeIn(tween(280)),
            exit = if (LocalReducedMotion.current) ExitTransition.None else shrinkHorizontally(tween(220)) + fadeOut(tween(160))
        ) {
            Text(
                dest.label,
                modifier = Modifier.padding(start = 8.dp),
                color = Black,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
