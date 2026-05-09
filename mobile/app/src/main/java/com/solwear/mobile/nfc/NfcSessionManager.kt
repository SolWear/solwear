package com.solwear.mobile.nfc

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter.EXTRA_NDEF_MESSAGES
import android.nfc.NfcAdapter.EXTRA_TAG
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.util.Log
import com.solwear.mobile.terminal.nfc.BridgeEvent
import com.solwear.mobile.terminal.nfc.SolWearHostApduService
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
    data class TapDetected(val source: String) : NfcResult
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
    private var foregroundActivity: Activity? = null

    fun startSession(mode: NfcMode, signRequestMessage: NdefMessage? = null) {
        _mode.value = mode
        pendingSignRequest = signRequestMessage
        _lastErrorDetail.value = null
        _result.value = null
        _state.value = NfcState.WAITING_FOR_TAG

        when (mode) {
            NfcMode.READ_WALLET -> {
                _lastErrorDetail.value = "Primary: reading SolWear as a Type 4 tag. Fallback: phone exposes sync_ping for the watch."
                SolWearHostApduService.startBridgeSyncPing { handleBridgeEvent(it) }
                updateReaderMode()
            }
            NfcMode.WRITE_SIGN_REQUEST -> {
                SolWearHostApduService.stopBridgeSession()
                val message = signRequestMessage
                if (message == null) {
                    emitError("No sign request data")
                    return
                }
                updateReaderMode()
            }
            NfcMode.READ_SIGN_RESPONSE -> {
                SolWearHostApduService.stopBridgeSession()
                updateReaderMode()
            }
        }
    }

    fun cancelSession() {
        _state.value = NfcState.IDLE
        _result.value = null
        _lastErrorDetail.value = null
        pendingSignRequest = null
        _mode.value = NfcMode.READ_WALLET
        updateReaderMode()
        SolWearHostApduService.stopBridgeSession()
    }

    fun enableForegroundDispatch(activity: Activity) {
        foregroundActivity = activity
        updateReaderMode()
    }

    fun disableForegroundDispatch(activity: Activity) {
        if (foregroundActivity === activity) {
            disableReaderMode()
            foregroundActivity = null
        }
    }

    fun handleIntent(intent: Intent) {
        Log.d(TAG, "NFC intent action=${intent.action}")
        val message = intentNdefMessage(intent)
        if (message != null && handleNdefMessage(message)) return

        @Suppress("DEPRECATION")
        val tag = intent.getParcelableExtra<Tag>(EXTRA_TAG)
        if (tag != null) {
            handleDiscoveredTag(tag)
        }
    }

    fun resetState() {
        _state.value = NfcState.IDLE
        _result.value = null
        _lastErrorDetail.value = null
        pendingSignRequest = null
        _mode.value = NfcMode.READ_WALLET
        updateReaderMode()
        SolWearHostApduService.stopBridgeSession()
    }

    private fun updateReaderMode() {
        val activity = foregroundActivity ?: return
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return

        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        adapter.enableReaderMode(
            activity,
            { tag -> handleDiscoveredTag(tag) },
            flags,
            null
        )
    }

    private fun disableReaderMode() {
        val activity = foregroundActivity ?: return
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        adapter.disableReaderMode(activity)
    }

    private fun handleDiscoveredTag(tag: Tag) {
        when (_mode.value) {
            NfcMode.READ_WALLET -> {
                _state.value = NfcState.READING
                val message = Type4IsoDepNdefReader.readNdefMessage(tag)
                if (message == null) {
                    emitError("SolWear watch detected, but no Type 4 wallet payload was readable")
                    return
                }
                if (!handleNdefMessage(message)) {
                    emitError("NFC tag is readable, but it is not a SolWear wallet tag")
                }
            }
            NfcMode.WRITE_SIGN_REQUEST -> {
                val message = pendingSignRequest
                if (message == null) {
                    emitError("No sign request data")
                    return
                }
                _state.value = NfcState.WRITING
                if (Type4IsoDepNdefReader.writeNdefMessage(tag, message)) {
                    _mode.value = NfcMode.READ_SIGN_RESPONSE
                    _state.value = NfcState.WAITING_FOR_TAG
                    _lastErrorDetail.value = "Request sent. Confirm on SolWear, then tap again."
                } else {
                    emitError("Could not write sign request to SolWear watch")
                }
            }
            NfcMode.READ_SIGN_RESPONSE -> {
                _state.value = NfcState.READING
                val message = Type4IsoDepNdefReader.readNdefMessage(tag)
                if (message == null) {
                    emitError("No signature response readable yet. Confirm on SolWear and tap again.")
                    return
                }
                val response = NdefProtocol.parseSignResponse(message)
                if (response != null) {
                    pendingSignRequest = null
                    _result.value = NfcResult.SignatureRead(response)
                    _state.value = NfcState.SUCCESS
                } else {
                    emitError("SolWear responded, but no signature payload was found")
                }
            }
        }
    }

    private fun handleNdefMessage(message: NdefMessage): Boolean {
        NdefProtocol.parseWalletMessage(message)?.let { payload ->
            _result.value = NfcResult.WalletRead(payload)
            _state.value = NfcState.SUCCESS
            return true
        }

        NdefProtocol.parseSyncPing(message)?.let { ping ->
            _result.value = NfcResult.TapDetected(ping.source)
            _state.value = NfcState.SUCCESS
            return true
        }

        NdefProtocol.parseSignResponse(message)?.let { response ->
            _result.value = NfcResult.SignatureRead(response)
            _state.value = NfcState.SUCCESS
            return true
        }

        return false
    }

    private fun intentNdefMessage(intent: Intent): NdefMessage? {
        @Suppress("DEPRECATION")
        val messages = intent.getParcelableArrayExtra(EXTRA_NDEF_MESSAGES)
        return messages?.filterIsInstance<NdefMessage>()?.firstOrNull()
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
            is BridgeEvent.TapDetected -> {
                _result.value = NfcResult.TapDetected(event.source)
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
        private const val TAG = "SolWearNfc"

        fun isNfcAvailable(activity: Activity): Boolean {
            return NfcAdapter.getDefaultAdapter(activity) != null
        }

        fun isNfcEnabled(activity: Activity): Boolean {
            return NfcAdapter.getDefaultAdapter(activity)?.isEnabled == true
        }
    }
}
