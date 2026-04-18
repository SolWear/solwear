package com.example.solvare.terminal.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.example.solvare.terminal.crypto.TerminalKeyManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

/**
 * HCE service emulating an NFC Forum Type 4 Tag.
 *
 * The phone (Solvare) reads NDEF from this service to get the wallet pubkey.
 * For signing, the phone writes a sign_request via UPDATE BINARY, then reads
 * back the sign_response on the next tap.
 */
class SolvareHostApduService : HostApduService() {

    private lateinit var keyManager: TerminalKeyManager

    private var ccFile: ByteArray = byteArrayOf()
    private var ndefFile: ByteArray = byteArrayOf()
    private var selectedFileId: Int = 0

    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        keyManager = TerminalKeyManager(applicationContext)
        ccFile = buildCcFile()
        // Между тапами система уничтожает сервис; чтобы sign_response дожил до следующего тапа, берём из companion.
        ndefFile = pendingNdefFile ?: buildWalletNdefFile()
        Log.d(TAG, "HCE service created. Pubkey: ${keyManager.publicKeyBase58}, pendingResponse=${pendingNdefFile != null}")
    }

    override fun onDestroy() {
        if (activeInstance === this) {
            activeInstance = null
        }
        super.onDestroy()
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (commandApdu.size < 4) return SW_UNKNOWN

        val ins = commandApdu[1].toInt() and 0xFF
        val p1 = commandApdu[2].toInt() and 0xFF
        val p2 = commandApdu[3].toInt() and 0xFF

        Log.d(TAG, "APDU: ${commandApdu.toHex()} (INS=0x${ins.toString(16)})")

        return when (ins) {
            INS_SELECT -> handleSelect(commandApdu, p1, p2)
            INS_READ_BINARY -> handleReadBinary(p1, p2, commandApdu)
            INS_UPDATE_BINARY -> handleUpdateBinary(p1, p2, commandApdu)
            else -> SW_INS_NOT_SUPPORTED
        }
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "HCE deactivated, reason=$reason")
    }

    private fun handleSelect(apdu: ByteArray, p1: Int, p2: Int): ByteArray {
        val lc = if (apdu.size > 4) apdu[4].toInt() and 0xFF else 0
        val data = if (lc > 0 && apdu.size >= 5 + lc) apdu.copyOfRange(5, 5 + lc) else byteArrayOf()

        if (p1 == 0x04) {
            if (data.contentEquals(NDEF_AID)) {
                Log.d(TAG, "SELECT NDEF Tag Application AID")
                selectedFileId = 0
                return SW_OK
            }
            return SW_FILE_NOT_FOUND
        }

        if (p1 == 0x00 && p2 == 0x0C && data.size == 2) {
            val fileId = (data[0].toInt() and 0xFF shl 8) or (data[1].toInt() and 0xFF)
            return when (fileId) {
                FILE_CC -> {
                    selectedFileId = FILE_CC
                    Log.d(TAG, "SELECT CC file")
                    SW_OK
                }
                FILE_NDEF -> {
                    selectedFileId = FILE_NDEF
                    Log.d(TAG, "SELECT NDEF file")
                    SW_OK
                }
                else -> SW_FILE_NOT_FOUND
            }
        }
        return SW_UNKNOWN
    }

    private fun handleReadBinary(p1: Int, p2: Int, apdu: ByteArray): ByteArray {
        val offset = (p1 shl 8) or p2
        val le = if (apdu.size > 4) apdu[apdu.size - 1].toInt() and 0xFF else 0

        val file = when (selectedFileId) {
            FILE_CC -> ccFile
            FILE_NDEF -> ndefFile
            else -> return SW_FILE_NOT_FOUND
        }

        if (offset >= file.size) return SW_WRONG_PARAMS

        val end = minOf(offset + le, file.size)
        val chunk = file.copyOfRange(offset, end)

        Log.d(TAG, "READ BINARY offset=$offset le=$le -> ${chunk.size} bytes")
        return chunk + SW_OK
    }

    private fun handleUpdateBinary(p1: Int, p2: Int, apdu: ByteArray): ByteArray {
        if (selectedFileId != FILE_NDEF) return SW_FILE_NOT_FOUND

        val offset = (p1 shl 8) or p2
        val lc = if (apdu.size > 4) apdu[4].toInt() and 0xFF else 0
        val data = if (lc > 0 && apdu.size >= 5 + lc) apdu.copyOfRange(5, 5 + lc) else byteArrayOf()

        Log.d(TAG, "UPDATE BINARY offset=$offset lc=$lc")
        if (data.isEmpty()) return SW_OK

        // Клиент Type 4 write: сначала обнуляет 2 байта длины (offset=0, 00 00), затем пишет тело
        // начиная с offset=2, наконец записывает реальную длину в offset=0. Поэтому НЕЛЬЗЯ заменять
        // весь файл при offset=0 — это стирает уже записанное тело. Всегда патчим с расширением.
        if (offset + data.size > ndefFile.size) {
            val expanded = ByteArray(offset + data.size)
            ndefFile.copyInto(expanded)
            ndefFile = expanded
        }
        data.copyInto(ndefFile, offset)

        tryProcessSignRequest()

        return SW_OK
    }

    private fun tryProcessSignRequest() {
        if (ndefFile.size < 2) return
        val ndefLen = (ndefFile[0].toInt() and 0xFF shl 8) or (ndefFile[1].toInt() and 0xFF)
        if (ndefFile.size < 2 + ndefLen || ndefLen == 0) return

        val ndefBytes = ndefFile.copyOfRange(2, 2 + ndefLen)

        try {
            val ndefMessage = android.nfc.NdefMessage(ndefBytes)
            for (record in ndefMessage.records) {
                val recordType = String(record.type)
                if (record.tnf == android.nfc.NdefRecord.TNF_EXTERNAL_TYPE &&
                    (recordType.equals("solvare:sign_request", ignoreCase = true) ||
                        recordType.equals("sign_request", ignoreCase = true))
                ) {
                    val payload = String(record.payload).trim()
                    if (payload.isEmpty()) {
                        Log.w(TAG, "sign_request record with empty payload")
                        return
                    }
                    Log.d(TAG, "Got sign_request: $payload")
                    val request = json.decodeFromString<SignRequest>(payload)
                    val txBytes = android.util.Base64.decode(request.tx_bytes, android.util.Base64.NO_WRAP)

                    val messageToSign = if (txBytes.size > 65) {
                        txBytes.copyOfRange(65, txBytes.size)
                    } else {
                        txBytes
                    }

                    val signature = keyManager.sign(messageToSign)
                    val sigBase64 = android.util.Base64.encodeToString(signature, android.util.Base64.NO_WRAP)

                    Log.d(TAG, "Signed! Sig: ${sigBase64.take(20)}...")

                    ndefFile = buildSignResponseNdefFile(sigBase64)
                    // Переживает onDestroy/onCreate между тапами: читатель подносит телефон заново.
                    pendingNdefFile = ndefFile

                    lastEvent = "Транзакцію підписано"
                    eventCallback?.invoke(lastEvent!!)
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing sign request", e)
            lastEvent = "Помилка підпису: ${e.message}"
            eventCallback?.invoke(lastEvent!!)
        }
    }

    private fun buildWalletNdefFile(): ByteArray {
        val walletPayload = json.encodeToString(
            WalletPayload(
                version = 1,
                pubkey = keyManager.publicKeyBase58,
                network = "devnet"
            )
        )
        val record = android.nfc.NdefRecord.createExternal("solvare", "wallet", walletPayload.toByteArray())
        val message = android.nfc.NdefMessage(arrayOf(record))
        val messageBytes = message.toByteArray()

        val out = ByteArrayOutputStream()
        out.write((messageBytes.size shr 8) and 0xFF)
        out.write(messageBytes.size and 0xFF)
        out.write(messageBytes)
        return out.toByteArray()
    }

    private fun buildSignResponseNdefFile(signatureBase64: String): ByteArray {
        val responsePayload = json.encodeToString(
            SignResponse(version = 1, signature = signatureBase64)
        )
        val record = android.nfc.NdefRecord.createExternal("solvare", "sign_response", responsePayload.toByteArray())
        val message = android.nfc.NdefMessage(arrayOf(record))
        val messageBytes = message.toByteArray()

        val out = ByteArrayOutputStream()
        out.write((messageBytes.size shr 8) and 0xFF)
        out.write(messageBytes.size and 0xFF)
        out.write(messageBytes)
        return out.toByteArray()
    }

    private fun buildCcFile(): ByteArray {
        val maxNdefSize = 1024
        return byteArrayOf(
            0x00, 0x0F,
            0x20,
            0x00, 0x3B,
            0x00, 0x34,
            0x04, 0x06,
            0xE1.toByte(), 0x04,
            (maxNdefSize shr 8).toByte(), (maxNdefSize and 0xFF).toByte(),
            0x00,
            0x00
        )
    }

    private fun resetToWalletMode() {
        pendingNdefFile = null
        ndefFile = buildWalletNdefFile()
        lastEvent = "Скидання в режим гаманця"
        eventCallback?.invoke(lastEvent!!)
        Log.d(TAG, "Reset to wallet mode")
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

    companion object {
        private const val TAG = "SolvareHCE"

        private const val INS_SELECT = 0xA4
        private const val INS_READ_BINARY = 0xB0
        private const val INS_UPDATE_BINARY = 0xD6

        private const val FILE_CC = 0xE103
        private const val FILE_NDEF = 0xE104

        private val NDEF_AID = byteArrayOf(
            0xD2.toByte(), 0x76, 0x00, 0x00,
            0x85.toByte(), 0x01, 0x01
        )

        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
        private val SW_FILE_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
        private val SW_WRONG_PARAMS = byteArrayOf(0x6B, 0x00)
        private val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D, 0x00)
        private val SW_UNKNOWN = byteArrayOf(0x6F, 0x00)

        var lastEvent: String? = null
        var eventCallback: ((String) -> Unit)? = null

        @Volatile
        private var activeInstance: SolvareHostApduService? = null

        /** Сохраняется между onDestroy/onCreate: sign_response должен дожить до следующего тапа. */
        @Volatile
        private var pendingNdefFile: ByteArray? = null

        /** Сброс NDEF на живом экземпляре HCE (если сервис уже поднят системой). */
        fun resetLiveServiceToWalletMode() {
            pendingNdefFile = null
            activeInstance?.resetToWalletMode()
        }
    }
}

@Serializable
data class WalletPayload(
    val version: Int = 1,
    val pubkey: String,
    val network: String = "devnet"
)

@Serializable
data class SignRequest(
    val version: Int = 1,
    val tx_bytes: String
)

@Serializable
data class SignResponse(
    val version: Int = 1,
    val signature: String
)
