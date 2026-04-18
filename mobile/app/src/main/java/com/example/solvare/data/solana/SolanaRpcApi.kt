package com.example.solvare.data.solana

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface SolanaRpcApi {
    @POST
    suspend fun call(@Url url: String, @Body request: RpcRequest): RpcResponse<JsonElement>
}
