package com.example.solvare.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DOMAIN = "solvare"
private const val TYPE_WALLET = "wallet"
private const val TYPE_SIGN_REQUEST = "sign_request"
private const val TYPE_SIGN_RESPONSE = "sign_response"

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class WalletPayload(
    val version: Int = 1,
    val pubkey: String,
    val network: String = "devnet"
)

@Serializable
data class SignRequest(
    val version: Int = 1,
    val tx_bytes: String  // base64-encoded transaction bytes
)

@Serializable
data class SignResponse(
    val version: Int = 1,
    val signature: String  // base64-encoded 64-byte ed25519 signature
)

object NdefProtocol {

    fun parseWalletMessage(message: NdefMessage): WalletPayload? {
        return message.records.firstNotNullOfOrNull { record ->
            if (isExternalType(record, TYPE_WALLET)) {
                try {
                    val raw = String(record.payload).trim()
                    if (raw.isEmpty()) return@firstNotNullOfOrNull null
                    json.decodeFromString<WalletPayload>(raw)
                } catch (_: Exception) {
                    null
                }
            } else null
        }
    }

    fun parseSignResponse(message: NdefMessage): SignResponse? {
        return message.records.firstNotNullOfOrNull { record ->
            if (isExternalType(record, TYPE_SIGN_RESPONSE)) {
                try {
                    val raw = String(record.payload).trim()
                    if (raw.isEmpty()) return@firstNotNullOfOrNull null
                    json.decodeFromString<SignResponse>(raw)
                } catch (_: Exception) {
                    null
                }
            } else null
        }
    }

    fun createSignRequestMessage(txBytes: ByteArray): NdefMessage {
        val payload = json.encodeToString(
            SignRequest(tx_bytes = android.util.Base64.encodeToString(txBytes, android.util.Base64.NO_WRAP))
        )
        val record = NdefRecord.createExternal(DOMAIN, TYPE_SIGN_REQUEST, payload.toByteArray())
        return NdefMessage(arrayOf(record))
    }

    private fun isExternalType(record: NdefRecord, type: String): Boolean {
        if (record.tnf != NdefRecord.TNF_EXTERNAL_TYPE) return false
        val recordType = String(record.type)
        return recordType.equals("$DOMAIN:$type", ignoreCase = true) ||
            recordType.equals(type, ignoreCase = true) ||
            // На части прошивок тип приходит с префиксом/суффиксом
            recordType.contains(":$type", ignoreCase = true)
    }
}
