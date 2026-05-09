package com.solwear.mobile.nfc

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * Читання NDEF з NFC Forum Type 4 (HCE SolWear), коли [android.nfc.tech.Ndef] недоступний або порожній
 * (типово для reader mode + деяких прошивок).
 */
internal object Type4IsoDepNdefReader {

    private const val TAG = "SolWearNfc"

    /** Той самий NDEF Tag Application, що в [com.solwear.mobile.terminal.nfc.SolWearHostApduService]. */
    private val NDEF_AID = byteArrayOf(
        0xD2.toByte(), 0x76, 0x00, 0x00,
        0x85.toByte(), 0x01, 0x01
    )

    private val SELECT_NDEF_APP = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x07
    ) + NDEF_AID

    private val SELECT_NDEF_FILE = byteArrayOf(
        0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0xE1.toByte(), 0x04
    )

    fun readNdefMessage(tag: Tag): NdefMessage? {
        val iso = IsoDep.get(tag) ?: return null
        return try {
            iso.connect()
            iso.timeout = 10_000
            if (!transceiveOk(iso, SELECT_NDEF_APP)) {
                Log.w(TAG, "IsoDep: SELECT NDEF AID failed")
                return null
            }
            if (!transceiveOk(iso, SELECT_NDEF_FILE)) {
                Log.w(TAG, "IsoDep: SELECT NDEF file E104 failed")
                return null
            }
            val raw = readNdefFileRaw(iso) ?: return null
            if (raw.size < 2) return null
            val ndefLen = (raw[0].toInt() and 0xFF shl 8) or (raw[1].toInt() and 0xFF)
            if (ndefLen <= 0 || raw.size < 2 + ndefLen) {
                Log.w(TAG, "IsoDep: некоректна довжина NDEF len=$ndefLen raw=${raw.size}")
                return null
            }
            val ndefBytes = raw.copyOfRange(2, 2 + ndefLen)
            NdefMessage(ndefBytes)
        } catch (e: Exception) {
            Log.w(TAG, "IsoDep: читання NDEF", e)
            null
        } finally {
            try {
                iso.close()
            } catch (_: Exception) {
            }
        }
    }

    fun writeNdefMessage(tag: Tag, message: NdefMessage): Boolean {
        val iso = IsoDep.get(tag) ?: return false
        return try {
            iso.connect()
            iso.timeout = 10_000
            if (!transceiveOk(iso, SELECT_NDEF_APP)) {
                Log.w(TAG, "IsoDep: SELECT NDEF AID failed before write")
                return false
            }
            if (!transceiveOk(iso, SELECT_NDEF_FILE)) {
                Log.w(TAG, "IsoDep: SELECT NDEF file E104 failed before write")
                return false
            }

            val ndefBytes = message.toByteArray()
            if (ndefBytes.size > 1024) {
                Log.w(TAG, "IsoDep: NDEF write too large (${ndefBytes.size})")
                return false
            }

            if (!updateBinary(iso, 0, byteArrayOf(0x00, 0x00))) return false
            var offset = 2
            var sent = 0
            while (sent < ndefBytes.size) {
                val len = minOf(220, ndefBytes.size - sent)
                if (!updateBinary(iso, offset, ndefBytes.copyOfRange(sent, sent + len))) {
                    return false
                }
                sent += len
                offset += len
            }
            val finalLen = byteArrayOf(
                ((ndefBytes.size shr 8) and 0xFF).toByte(),
                (ndefBytes.size and 0xFF).toByte()
            )
            updateBinary(iso, 0, finalLen)
        } catch (e: Exception) {
            Log.w(TAG, "IsoDep: writing NDEF", e)
            false
        } finally {
            try {
                iso.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun readNdefFileRaw(iso: IsoDep): ByteArray? {
        val out = ByteArrayOutputStream()
        var offset = 0
        var targetSize = 2048
        repeat(24) {
            if (offset >= targetSize) return@repeat
            val le = minOf(220, targetSize - offset)
            val p1 = (offset shr 8) and 0xFF
            val p2 = offset and 0xFF
            val cmd = byteArrayOf(
                0x00,
                0xB0.toByte(),
                p1.toByte(),
                p2.toByte(),
                le.toByte()
            )
            val resp = iso.transceive(cmd)
            if (resp.size < 2) return null
            if (!isOkSw(resp)) {
                Log.w(TAG, "IsoDep: READ BINARY offset=$offset sw=${resp.copyOfRange(resp.size - 2, resp.size).toHex()}")
                return null
            }
            val data = resp.copyOfRange(0, resp.size - 2)
            if (data.isEmpty()) return null
            out.write(data)
            offset += data.size
            val buf = out.toByteArray()
            if (buf.size >= 2) {
                val ndefContentLen = (buf[0].toInt() and 0xFF shl 8) or (buf[1].toInt() and 0xFF)
                targetSize = 2 + ndefContentLen
                if (buf.size >= targetSize) return buf
            }
        }
        return out.toByteArray().takeIf { it.isNotEmpty() }
    }

    private fun transceiveOk(iso: IsoDep, cmd: ByteArray): Boolean {
        val resp = iso.transceive(cmd)
        return isOkSw(resp)
    }

    private fun updateBinary(iso: IsoDep, offset: Int, data: ByteArray): Boolean {
        if (data.size > 255) return false
        val cmd = byteArrayOf(
            0x00,
            0xD6.toByte(),
            ((offset shr 8) and 0xFF).toByte(),
            (offset and 0xFF).toByte(),
            data.size.toByte()
        ) + data
        val resp = iso.transceive(cmd)
        if (!isOkSw(resp)) {
            Log.w(TAG, "IsoDep: UPDATE BINARY offset=$offset sw=${resp.takeLast(2).toByteArray().toHex()}")
            return false
        }
        return true
    }

    private fun isOkSw(resp: ByteArray): Boolean {
        if (resp.size < 2) return false
        return resp[resp.size - 2] == 0x90.toByte() && resp[resp.size - 1] == 0x00.toByte()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }
}
