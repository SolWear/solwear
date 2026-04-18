package com.example.solvare.nfc

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.IsoDep
import android.os.Build
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

    /** Текст останньої помилки для UI та збіг з `adb logcat -s SolvareNfc`. */
    private val _lastErrorDetail = MutableStateFlow<String?>(null)
    val lastErrorDetail: StateFlow<String?> = _lastErrorDetail.asStateFlow()

    private var currentMode: NfcMode = NfcMode.READ_WALLET
    private var pendingSignRequest: NdefMessage? = null

    private val _mode = MutableStateFlow(NfcMode.READ_WALLET)
    val mode: StateFlow<NfcMode> = _mode.asStateFlow()

    fun startSession(mode: NfcMode, signRequestMessage: NdefMessage? = null) {
        currentMode = mode
        _mode.value = mode
        pendingSignRequest = signRequestMessage
        _lastErrorDetail.value = null
        _state.value = NfcState.WAITING_FOR_TAG
        _result.value = null
    }

    fun cancelSession() {
        _state.value = NfcState.IDLE
        _result.value = null
        _lastErrorDetail.value = null
        pendingSignRequest = null
        currentMode = NfcMode.READ_WALLET
        _mode.value = NfcMode.READ_WALLET
    }

    fun enableForegroundDispatch(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Без PendingIntent → немає BAL при доставці від NFC-сервера; ActivityOptions при створенні PI заборонені.
            adapter.enableReaderMode(
                activity,
                { tag ->
                    // Не переключаемся на UI: IsoDep/Ndef I/O обязательно вне главного потока.
                    val synthetic = Intent().apply {
                        putExtra(NfcAdapter.EXTRA_TAG, tag)
                    }
                    handleIntent(synthetic)
                },
                READER_MODE_FLAGS,
                null
            )
            return
        }
        val intent = Intent(activity, activity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = buildPendingIntentFlagsForNfcForegroundDispatch()
        @Suppress("DEPRECATION")
        val pendingIntent = android.app.PendingIntent.getActivity(
            activity,
            NFC_DISPATCH_REQUEST_CODE,
            intent,
            flags
        )
        adapter.enableForegroundDispatch(activity, pendingIntent, null, null)
    }

    fun disableForegroundDispatch(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            adapter.disableReaderMode(activity)
        } else {
            adapter.disableForegroundDispatch(activity)
        }
    }

    fun handleIntent(intent: Intent) {
        if (_state.value != NfcState.WAITING_FOR_TAG) return

        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: run {
            emitError("NFC-тег не знайдено")
            return
        }

        when (currentMode) {
            NfcMode.READ_WALLET -> readWallet(intent, tag)
            NfcMode.WRITE_SIGN_REQUEST -> writeSignRequest(tag)
            NfcMode.READ_SIGN_RESPONSE -> readSignResponse(intent, tag)
        }
    }

    private fun readWallet(intent: Intent, tag: Tag) {
        _state.value = NfcState.READING
        try {
            val ndefMessage = extractNdefMessage(intent, tag) ?: run {
                emitError("NDEF-повідомлення не знайдено")
                return
            }
            val payload = NdefProtocol.parseWalletMessage(ndefMessage)
            if (payload != null) {
                _result.value = NfcResult.WalletRead(payload)
                _state.value = NfcState.SUCCESS
            } else {
                emitError("Не вдалося прочитати дані гаманця")
            }
        } catch (e: Exception) {
            emitError("Помилка читання NFC: ${describeThrowable(e)}", e)
        }
    }

    private fun writeSignRequest(tag: Tag) {
        _state.value = NfcState.WRITING
        val message = pendingSignRequest ?: run {
            emitError("Немає даних для підпису")
            return
        }
        try {
            val ndef = Ndef.get(tag)
            if (ndef == null) {
                emitError("Тег не підтримує NDEF (потрібен Solvare Watch)")
                return
            }
            ndef.connect()
            try {
                ndef.writeNdefMessage(message)
            } finally {
                try {
                    ndef.close()
                } catch (_: Exception) {
                }
            }
            _state.value = NfcState.SUCCESS
            currentMode = NfcMode.READ_SIGN_RESPONSE
            _mode.value = NfcMode.READ_SIGN_RESPONSE
            _state.value = NfcState.WAITING_FOR_TAG
        } catch (e: Exception) {
            emitError("Помилка запису NFC: ${describeThrowable(e)}", e)
        }
    }

    private fun readSignResponse(intent: Intent, tag: Tag) {
        _state.value = NfcState.READING
        try {
            val ndefMessage = extractNdefMessage(intent, tag) ?: run {
                emitError("Відповідь підпису не отримано (немає NDEF в intent і на тегу)")
                return
            }
            val response = NdefProtocol.parseSignResponse(ndefMessage)
            if (response != null) {
                _result.value = NfcResult.SignatureRead(response)
                _state.value = NfcState.SUCCESS
            } else {
                emitError("Не вдалося прочитати підпис")
            }
        } catch (e: Exception) {
            emitError("Помилка читання підпису: ${describeThrowable(e)}", e)
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

    /** IOException и др. часто дают message == null — показываем класс и подсказку. */
    private fun describeThrowable(e: Throwable): String {
        val detail = e.message?.takeIf { it.isNotBlank() }
            ?: e.cause?.message?.takeIf { it.isNotBlank() }
        val base = buildString {
            append(e.javaClass.simpleName)
            if (detail != null) append(": ").append(detail)
        }
        return if (detail == null) {
            "$base (часто обрив зв'язку — піднесіть годинник ще раз; див. підказку про NFC та оплату)"
        } else {
            base
        }
    }

    /**
     * API 33+ требует типизированный getParcelableArrayExtra; иначе EXTRA_NDEF_MESSAGES часто null.
     * Для HCE Type 4: спочатку [Ndef], якщо немає даних — [IsoDep] (той самий формат файлу, що в SolvareHostApduService).
     */
    private fun extractNdefMessage(intent: Intent, tag: Tag): NdefMessage? {
        val fromIntent = readNdefMessagesFromIntent(intent).firstOrNull()
        if (fromIntent != null) return fromIntent

        val ndefTech = Ndef.get(tag)
        if (ndefTech != null) {
            try {
                ndefTech.connect()
                try {
                    val msg = ndefTech.cachedNdefMessage ?: ndefTech.ndefMessage
                    if (msg != null) return msg
                } finally {
                    try {
                        ndefTech.close()
                    } catch (_: Exception) {
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Ndef: читання не вдалося, пробуємо IsoDep", e)
            }
        } else {
            Log.d(TAG, "Ndef.get(tag)==null; techs=${tag.techList.joinToString()} IsoDep=${IsoDep.get(tag) != null}")
        }

        return Type4IsoDepNdefReader.readNdefMessage(tag)
    }

    private fun readNdefMessagesFromIntent(intent: Intent): List<NdefMessage> {
        if (Build.VERSION.SDK_INT >= 33) {
            val typed = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES,
                NdefMessage::class.java
            )?.filterIsInstance<NdefMessage>().orEmpty()
            if (typed.isNotEmpty()) return typed
        }
        @Suppress("DEPRECATION")
        return intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            ?.mapNotNull { it as? NdefMessage }.orEmpty()
    }

    fun resetState() {
        _state.value = NfcState.IDLE
        _result.value = null
        _lastErrorDetail.value = null
        currentMode = NfcMode.READ_WALLET
        _mode.value = NfcMode.READ_WALLET
    }

    companion object {
        private const val TAG = "SolvareNfc"

        /** Смена requestCode сбрасывает закэшированный PI без BAL-opt-in после обновления приложения. */
        private const val NFC_DISPATCH_REQUEST_CODE = 0x4e46_4352 // "NFCQ" + 1

        private val READER_MODE_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        private fun buildPendingIntentFlagsForNfcForegroundDispatch(): Int {
            var f = android.app.PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                f = f or android.app.PendingIntent.FLAG_MUTABLE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // PendingIntent.FLAG_ALLOW_BACKGROUND_ACTIVITY_STARTS (API 34); literal — сумісність зі stubs
                f = f or 0x01000000
            }
            return f
        }

        fun isNfcAvailable(activity: Activity): Boolean {
            return NfcAdapter.getDefaultAdapter(activity) != null
        }

        fun isNfcEnabled(activity: Activity): Boolean {
            return NfcAdapter.getDefaultAdapter(activity)?.isEnabled == true
        }
    }
}
