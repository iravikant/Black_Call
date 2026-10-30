package com.blackcall.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.blackcall.container
import com.blackcall.di.AppContainer
import com.blackcall.domain.BlockReason
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Creates a ViewModel with the app container, no DI framework required. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = LocalContext.current.container
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}

val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState not provided")
}

fun BlockReason.icon(): ImageVector = when (this) {
    BlockReason.MANUAL -> Icons.Rounded.Block
    BlockReason.PREFIX -> Icons.Rounded.Dialpad
    BlockReason.SCHEDULE -> Icons.Rounded.Bedtime
    BlockReason.NOT_ALLOWED -> Icons.Rounded.Contacts
    BlockReason.HIDDEN -> Icons.Rounded.VisibilityOff
    BlockReason.SPOOFED -> Icons.Rounded.GppBad
    BlockReason.UNKNOWN -> Icons.Rounded.PersonOff
    BlockReason.SPAM -> Icons.Rounded.Report
}

object Format {
    private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")
    private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM")

    fun time(epochMs: Long): String =
        timeFormat.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))

    fun day(epochMs: Long): String {
        val d = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        return when (d) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> dayFormat.format(d)
        }
    }

    fun minuteOfDay(minutes: Int): String =
        timeFormat.format(LocalTime.of((minutes / 60) % 24, minutes % 60))
}
