package com.blackcall.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.blackcall.container
import com.blackcall.domain.BlockAction
import com.blackcall.domain.Decision
import com.blackcall.domain.IncomingCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

/**
 * Screens every incoming call against the user's rules.
 *
 * Telecom gives us a few seconds to answer, so decisions use the in-memory rule snapshot
 * and fail open: if anything is slow or throws, the call rings normally.
 */
class BlackCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }
        scope.launch {
            val response = try {
                withTimeoutOrNull(SCREEN_TIMEOUT_MS) { screen(callDetails) }
            } catch (e: Exception) {
                Log.e(TAG, "Screening failed, allowing call", e)
                null
            } ?: CallResponse.Builder().build()
            respondToCall(callDetails, response)
        }
    }

    private suspend fun screen(details: Call.Details): CallResponse {
        val container = applicationContext.container
        val snapshot = container.snapshot.filterNotNull().first()

        val hidden = details.handlePresentation == TelecomManager.PRESENTATION_RESTRICTED
        val raw = if (hidden) null else details.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }

        val call = IncomingCall(
            rawNumber = raw,
            callerLabel = details.callerDisplayName,
            isEmergency = raw != null && isEmergency(raw),
            isSpoofed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                details.callerNumberVerificationStatus == Connection.VERIFICATION_STATUS_FAILED,
            minuteOfDay = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        )

        // Contact lookup is a blocking ContentResolver query.
        val decision = withContext(Dispatchers.IO) { container.engine.decide(call, snapshot) }

        return when (decision) {
            is Decision.Allow -> CallResponse.Builder().build()
            is Decision.Block -> {
                val action = snapshot.settings.action
                // appScope outlives this service, so logging finishes even if Telecom unbinds us.
                container.appScope.launch {
                    container.repository.logBlock(raw, action, decision)
                    if (snapshot.settings.notifyOnBlock) {
                        container.notifier.notifyBlocked(raw, decision.reason, action)
                    }
                }
                buildBlockResponse(action)
            }
        }
    }

    private fun buildBlockResponse(action: BlockAction): CallResponse =
        CallResponse.Builder().apply {
            when (action) {
                BlockAction.REJECT -> {
                    setDisallowCall(true)
                    setRejectCall(true)
                    setSkipNotification(true) // we post our own, richer notification
                }
                // Silenced calls still show as missed, so the system notification stays on.
                BlockAction.SILENCE -> setSilenceCall(true)
            }
            setSkipCallLog(false) // keep blocked calls visible in the system call log
        }.build()

    private fun isEmergency(number: String): Boolean {
        val digits = number.filter { it.isDigit() }
        if (digits in FALLBACK_EMERGENCY) return true
        return runCatching { PhoneNumberUtils.isEmergencyNumber(number) }.getOrDefault(false)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "BlackCallScreening"
        const val SCREEN_TIMEOUT_MS = 3_500L
        val FALLBACK_EMERGENCY = setOf("100", "101", "102", "108", "112", "911", "999", "000")
    }
}
