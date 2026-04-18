package com.example.solvare.data.price

import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PriceRepository(private val api: PriceApi) {

    private var cachedPrice: Double? = null
    private var lastFetchTime: Long = 0

    suspend fun getSolPriceUsd(): Result<Double> = runCatching {
        val now = System.currentTimeMillis()
        val cached = cachedPrice
        if (cached != null && now - lastFetchTime < CACHE_TTL_MS) {
            return@runCatching cached
        }

        try {
            val response = api.getPrice()
            val price = response["solana"]
                ?.jsonObject?.get("usd")
                ?.jsonPrimitive?.double
                ?: throw Exception("Invalid price response")
            cachedPrice = price
            lastFetchTime = now
            price
        } catch (e: Exception) {
            if (cached != null) cached
            else throw e
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 60_000L
        const val COINGECKO_BASE_URL = "https://api.coingecko.com/"
    }
}
