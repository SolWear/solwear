package com.example.solvare.data.solana

import kotlinx.coroutines.delay
import kotlinx.serialization.json.*

class SolanaRepository(private val api: SolanaRpcApi) {

    /**
     * Только официальный devnet: сторонние «бесплатные» RPC часто режут методы
     * (Ankr — ключ, dRPC — getBalance только на paid) или отдают 400.
     * Несколько повторов на таймаутах/пустых ответах.
     */
    private suspend fun rpcCall(request: RpcRequest): RpcResponse<JsonElement> {
        val url = "${DEVNET_URL.trimEnd('/')}/"
        var last: Exception? = null
        repeat(RPC_ATTEMPTS) { attempt ->
            try {
                return api.call(url, request)
            } catch (e: Exception) {
                last = e as? Exception ?: Exception(e.message, e)
                if (attempt < RPC_ATTEMPTS - 1) {
                    delay(300L shl attempt)
                }
            }
        }
        throw last ?: Exception("Solana RPC недоступний")
    }

    suspend fun getBalance(pubkey: String): Result<Double> = runCatching {
        val request = RpcRequest(
            method = "getBalance",
            params = listOf(JsonPrimitive(pubkey))
        )
        val response = rpcCall(request)
        if (response.error != null) {
            throw Exception("RPC: ${response.error.message} (код ${response.error.code})")
        }
        val result = response.result
        if (result == null || result is JsonNull) {
            throw Exception(
                "RPC getBalance: у відповіді немає result (порожній або некоректний JSON від вузла). Повторіть за хвилину."
            )
        }
        val valueEl = result.jsonObject["value"]
            ?: throw Exception("У відповіді getBalance немає поля value")
        val lamports = when (valueEl) {
            is JsonPrimitive -> valueEl.longOrNull
                ?: valueEl.doubleOrNull?.toLong()
                ?: valueEl.intOrNull?.toLong()
            else -> null
        } ?: throw Exception("Некоректне значення balance у відповіді RPC")
        lamports / 1_000_000_000.0
    }

    suspend fun getLatestBlockhash(): Result<BlockhashValue> = runCatching {
        val request = RpcRequest(
            method = "getLatestBlockhash",
            params = listOf(buildJsonObject {
                put("commitment", "finalized")
            })
        )
        val response = rpcCall(request)
        if (response.error != null) {
            throw Exception("RPC: ${response.error.message} (код ${response.error.code})")
        }
        val result = response.result
        if (result == null || result is JsonNull) {
            throw Exception("Відповідь getLatestBlockhash без result. Повторіть пізніше.")
        }
        val value = result.jsonObject["value"]?.jsonObject
            ?: throw Exception("Некоректна відповідь blockhash")
        BlockhashValue(
            blockhash = value["blockhash"]?.jsonPrimitive?.content
                ?: throw Exception("Немає blockhash"),
            lastValidBlockHeight = value["lastValidBlockHeight"]?.jsonPrimitive?.long ?: 0
        )
    }

    suspend fun sendTransaction(signedTxBase64: String): Result<String> = runCatching {
        val request = RpcRequest(
            method = "sendTransaction",
            params = listOf(
                JsonPrimitive(signedTxBase64),
                buildJsonObject {
                    put("encoding", "base64")
                    put("preflightCommitment", "confirmed")
                }
            )
        )
        val response = rpcCall(request)
        if (response.error != null) {
            throw Exception("RPC: ${response.error.message} (код ${response.error.code})")
        }
        response.result?.jsonPrimitive?.content
            ?: throw Exception("Немає підпису транзакції у відповіді sendTransaction")
    }

    suspend fun getSignatureStatus(signature: String): Result<SignatureStatus?> = runCatching {
        val request = RpcRequest(
            method = "getSignatureStatuses",
            params = listOf(buildJsonArray { add(JsonPrimitive(signature)) })
        )
        val response = rpcCall(request)
        if (response.error != null) {
            throw Exception("RPC: ${response.error.message} (код ${response.error.code})")
        }
        val result = response.result
        if (result == null || result is JsonNull) {
            return@runCatching null
        }
        val values = result.jsonObject["value"]?.jsonArray
        if (values.isNullOrEmpty() || values[0] is JsonNull) return@runCatching null
        val statusObj = values[0].jsonObject
        SignatureStatus(
            slot = statusObj["slot"]?.jsonPrimitive?.longOrNull,
            confirmations = statusObj["confirmations"]?.jsonPrimitive?.intOrNull,
            err = statusObj["err"],
            confirmationStatus = statusObj["confirmationStatus"]?.jsonPrimitive?.contentOrNull
        )
    }

    companion object {
        /** База для Retrofit; обязательно с завершающим `/`. */
        const val DEVNET_URL_PRIMARY = "https://api.devnet.solana.com/"

        /** Корень без слежа (логика, документация). */
        const val DEVNET_URL = "https://api.devnet.solana.com"

        const val MAINNET_URL = "https://api.mainnet-beta.solana.com"
        const val LAMPORTS_PER_SOL = 1_000_000_000L

        private const val RPC_ATTEMPTS = 4
    }
}
