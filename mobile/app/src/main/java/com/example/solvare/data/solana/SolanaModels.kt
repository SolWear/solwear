package com.example.solvare.data.solana

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String,
    val params: List<kotlinx.serialization.json.JsonElement> = emptyList()
)

@Serializable
data class RpcResponse<T>(
    val jsonrpc: String = "2.0",
    /** JSON-RPC id может быть числом, строкой или null — не фиксируем в Int. */
    val id: JsonElement? = null,
    val result: T? = null,
    val error: RpcError? = null
)

@Serializable
data class RpcError(
    val code: Int,
    val message: String
)

@Serializable
data class BalanceResult(
    val context: RpcContext,
    val value: Long
)

@Serializable
data class RpcContext(
    val slot: Long = 0
)

@Serializable
data class BlockhashResult(
    val context: RpcContext,
    val value: BlockhashValue
)

@Serializable
data class BlockhashValue(
    val blockhash: String,
    val lastValidBlockHeight: Long
)

@Serializable
data class SendTransactionResult(
    val value: String? = null
)

@Serializable
data class SignatureStatusResult(
    val context: RpcContext,
    val value: List<SignatureStatus?>
)

@Serializable
data class SignatureStatus(
    val slot: Long? = null,
    val confirmations: Int? = null,
    val err: kotlinx.serialization.json.JsonElement? = null,
    @SerialName("confirmationStatus")
    val confirmationStatus: String? = null
)
