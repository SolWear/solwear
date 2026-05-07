package com.example.solvare.nfc

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.util.Log
import com.example.solvare.terminal.nfc.BridgeEvent
import com.example.solvare.terminal.nfc.SolvareHostApduService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NfcState {
    IDLE,
    WAITING_FOR_TAG,
    READING,
    WRITING,
    SUCCESS,
    ERROR
}

sealed interface NfcResult {
    data class WalletRead(val payload: WalletPayload) : NfcResult
    data class SignatureRead(val response: SignResponse) : NfcResult
    data class Failure(val message: String) : NfcResult
}

enum class NfcMode {
    READ_WALLET,
    WRITE_SIGN_REQUEST,
    READ_SIGN_RESPONSE
}

class NfcSessionManager {

    private val _state = MutableStateFlow(NfcState.IDLE)
    val state: StateFlow<NfcState> = _state.asStateFlow()

    private val _result = MutableStateFlow<NfcResult?>(null)
    val result: StateFlow<NfcResult?> = _result.asStateFlow()

    private val _lastErrorDetail = MutableStateFlow<String?>(null)
    val lastErrorDetail: StateFlow<String?> = _lastErrorDetail.asStateFlow()

    private val _mode = MutableStateFlow(NfcMode.READ_WALLET)
    val mode: StateFlow<NfcMode> = _mode.asStateFlow()

    private var pendingSignRequest: NdefMessage? = null

    fun startSession(mode: NfcMode, signRequestMessage: NdefMessage? = null) {
        _mode.value = mode
        pendingSignRequest = signRequestMessage
        _lastErrorDetail.value = null
        _result.value = null
        _state.value = NfcState.WAITING_FOR_TAG

        when (mode) {
            NfcMode.READ_WALLET -> {
                SolvareHostApduService.startBridgeWalletRead(::handleBridgeEvent)
            }
            NfcMode.WRITE_SIGN_REQUEST -> {
                val message = signRequestMessage
                if (message == null) {
                    emitError("No sign request data")
                    return
                }
                SolvareHostApduService.startBridgeSignRequest(message, ::handleBridgeEvent)
                _mode.value = NfcMode.READ_SIGN_RESPONSE
            }
            NfcMode.READ_SIGN_RESPONSE -> Unit
        }
    }

    fun cancelSession() {
        _state.value = NfcState.IDLE
        _result.value = null
        _lastErrorDetail.value = null
        pendingSignRequest = null
        _mode.value = NfcMode.READ_WALLET
        SolvareHostApduService.stopBridgeSession()
    }

    fun enableForegroundDispatch(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        val component = ComponentName(activity, SolvareHostApduService::class.java)
        try {
            CardEmulation.getInstance(adapter).setPreferredService(activity, component)
        } catch (e: Exception) {
            Log.w(TAG, "HCE preferred service setup failed", e)
        }
    }

    fun disableForegroundDispatch(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        try {
            CardEmulation.getInstance(adapter).unsetPreferredService(activity)
        } catch (e: Exception) {
            Log.w(TAG, "HCE preferred service cleanup failed", e)
        }
    }

    fun handleIntent(intent: Intent) {
        Log.d(TAG, "Ignoring reader-mode NFC intent action=${intent.action}; SolWear uses HCE bridge")
    }

    fun resetState() {
        _state.value = NfcState.IDLE
        _result.value = null
        _lastErrorDetail.value = null
        pendingSignRequest = null
        _mode.value = NfcMode.READ_WALLET
        SolvareHostApduService.stopBridgeSession()
    }

    private fun handleBridgeEvent(event: BridgeEvent) {
        when (event) {
            is BridgeEvent.WalletRead -> {
                _result.value = NfcResult.WalletRead(
                    WalletPayload(pubkey = event.pubkey, network = event.network)
                )
                _state.value = NfcState.SUCCESS
            }
            is BridgeEvent.SignatureRead -> {
                _result.value = NfcResult.SignatureRead(
                    SignResponse(signature = event.signature)
                )
                _state.value = NfcState.SUCCESS
            }
            is BridgeEvent.Failure -> emitError(event.message)
        }
    }

    private fun emitError(message: String, cause: Throwable? = null) {
        if (cause != null) {
            Log.e(TAG, message, cause)
        } else {
            Log.e(TAG, message)
        }
        _lastErrorDetail.value = message
        _result.value = NfcResult.Failure(message)
        _state.value = NfcState.ERROR
    }

    companion object {
        private const val TAG = "SolvareNfc"

        fun isNfcAvailable(activity: Activity): Boolean {
            return NfcAdapter.getDefaultAdapter(activity) != null
        }

        fun isNfcEnabled(activity: Activity): Boolean {
            return NfcAdapter.getDefaultAdapter(activity)?.isEnabled == true
        }
    }
}
