package com.example.solvare.data.price

import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

interface PriceApi {
    @GET("api/v3/simple/price")
    suspend fun getPrice(
        @Query("ids") ids: String = "solana",
        @Query("vs_currencies") vsCurrencies: String = "usd"
    ): JsonObject
}
