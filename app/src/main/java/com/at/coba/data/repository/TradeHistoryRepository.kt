package com.at.coba.data.repository

import android.content.Context
import com.at.coba.data.network.ApiClient
import com.at.coba.ui.screens.HistoryItem
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.Instant

/**
 * Mengambil riwayat deal dari REST Stockity [`/bo-deals-history/v3/deals/trade`](https://api.stockity.id/...).
 * Bentuk resmi: `{ "success": true, "data": { "standard_trade_deals": [ ... ] } }`.
 */
object TradeHistoryRepository {

    /** API mengirim amount/profit dalam satuan ×100 (mis. minor unit); bagi untuk tampilan util. */
    private const val AMOUNT_UTIL_SCALE = 100.0

    suspend fun fetchTradeDeals(
        context: Context,
        type: String,
        locale: String = "id"
    ): Result<List<HistoryItem>> {
        return try {
            val raw = ApiClient.getApiService(context).getTradeDealsRaw(type = type, locale = locale).string()
            val root = JsonParser.parseString(raw)
            val list = when {
                root.isJsonArray -> root.asJsonArray
                root.isJsonObject -> extractArrayFromObject(root.asJsonObject)
                else -> null
            } ?: return Result.success(emptyList())

            val items = list.mapNotNull { el ->
                if (!el.isJsonObject) return@mapNotNull null
                mapDealObject(el.asJsonObject, type)
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractArrayFromObject(o: JsonObject): JsonArray? {
        val direct = listOf("records", "trades", "deals", "items", "data")
        for (key in direct) {
            if (!o.has(key)) continue
            val v = o.get(key)
            when {
                v.isJsonArray -> return v.asJsonArray
                v.isJsonObject -> {
                    val inner = v.asJsonObject
                    for (k2 in listOf(
                        "standard_trade_deals",
                        "records",
                        "trades",
                        "deals",
                        "items"
                    )) {
                        if (inner.has(k2) && inner.get(k2).isJsonArray) {
                            return inner.getAsJsonArray(k2)
                        }
                    }
                }
            }
        }
        // Root/object tanpa wrapper "data" (jarang)
        if (o.has("standard_trade_deals") && o.get("standard_trade_deals").isJsonArray) {
            return o.getAsJsonArray("standard_trade_deals")
        }
        return null
    }

    private fun mapDealObject(o: JsonObject, accountTypeParam: String): HistoryItem? {
        val id = o.firstLong("id", "deal_id") ?: return null

        val pair = o.firstString("asset_ric", "ric", "symbol", "pair", "instrument", "asset_name", "name")
            ?: "—"

        val statusRaw = o.firstString("status", "result", "outcome", "deal_status")?.lowercase()
            ?: "tie"
        val status = when {
            statusRaw.contains("win") || statusRaw == "won" -> "won"
            statusRaw.contains("lose") || statusRaw == "lost" -> "lost"
            statusRaw.contains("tie") || statusRaw == "draw" || statusRaw == "equal" -> "tie"
            else -> statusRaw
        }

        val trend = o.firstString("trend", "direction", "side")?.lowercase()
        val typeLabel = when (trend) {
            "call", "buy", "up" -> "BUY"
            "put", "sell", "down" -> "SELL"
            else -> {
                val rawType = o.firstString("type")?.uppercase()
                rawType?.takeIf { it in setOf("BUY", "SELL") } ?: "BUY"
            }
        }

        val dealType = o.firstString("deal_type")?.lowercase()
        val accountMode = when (dealType) {
            "demo" -> "Demo"
            "real" -> "Real"
            else -> when (accountTypeParam.lowercase()) {
                "demo" -> "Demo"
                "real" -> "Real"
                else -> o.firstString("wallet_type", "account_type", "mode")?.replaceFirstChar { it.uppercase() }
                    ?: "Real"
            }
        }

        val currency = o.firstString("currency", "wallet_currency", "iso_currency", "ccy") ?: "IDR"

        val amount = o.firstDouble("amount", "investment", "sum", "volume", "stake")
            ?: o.firstDouble("money", "bet")
            ?: 0.0

        val payout = o.firstDouble("win", "payment", "payout")
        val profitRaw = if (payout != null) {
            payout - amount
        } else {
            o.firstDouble("profit", "profit_amount", "gain", "result_amount")
                ?: o.firstDouble("pl", "pnl")
                ?: 0.0
        }

        // Tampilan & urutan mengikuti waktu tutup deal, buka hanya sebagai fallback.
        val createdMs = o.firstTimeMillis(
            "closed_at",
            "finished_at",
            "completed_at",
            "deal_closed_at",
            "close_time",
            "ended_at",
            "settled_at",
            "opened_at",
            "created_at"
        )
            ?: System.currentTimeMillis()

        val serverUuid = o.firstString(
            "uuid",
            "deal_uuid",
            "dealUuid",
            "guid",
            "external_uuid",
        )

        return HistoryItem(
            id = id,
            pair = pair,
            status = status,
            type = typeLabel,
            accountMode = accountMode,
            currency = currency,
            amount = amount / AMOUNT_UTIL_SCALE,
            profit = profitRaw / AMOUNT_UTIL_SCALE,
            createdAt = createdMs,
            serverUuid = serverUuid,
        )
    }

    private fun JsonObject.firstString(vararg keys: String): String? {
        for (k in keys) {
            if (!has(k) || get(k).isJsonNull) continue
            val e = get(k)
            when {
                e.isJsonPrimitive && e.asJsonPrimitive.isString -> return e.asString
                e.isJsonPrimitive && e.asJsonPrimitive.isNumber -> return e.asNumber.toString()
            }
        }
        return null
    }

    private fun JsonObject.firstDouble(vararg keys: String): Double? {
        for (k in keys) {
            if (!has(k) || get(k).isJsonNull) continue
            val e = get(k)
            try {
                when {
                    e.isJsonPrimitive && e.asJsonPrimitive.isNumber -> return e.asDouble
                    e.isJsonPrimitive && e.asJsonPrimitive.isString -> return e.asString.toDoubleOrNull()
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun JsonObject.firstLong(vararg keys: String): Long? {
        for (k in keys) {
            if (!has(k) || get(k).isJsonNull) continue
            val e = get(k)
            try {
                when {
                    e.isJsonPrimitive && e.asJsonPrimitive.isNumber -> return e.asLong
                    e.isJsonPrimitive && e.asJsonPrimitive.isString -> return e.asString.toLongOrNull()
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun JsonObject.firstTimeMillis(vararg keys: String): Long? {
        for (k in keys) {
            if (!has(k) || get(k).isJsonNull) continue
            val e = get(k)
            try {
                when {
                    e.isJsonPrimitive && e.asJsonPrimitive.isString -> {
                        val s = e.asString.trim()
                        val millis = try {
                            Instant.parse(s).toEpochMilli()
                        } catch (_: Exception) {
                            null
                        } ?: s.toLongOrNull()?.let { n ->
                            if (n > 1_000_000_000_000L) n else n * 1000L
                        }
                        if (millis != null) return millis
                    }
                    e.isJsonPrimitive && e.asJsonPrimitive.isNumber -> {
                        val n = e.asLong
                        return if (n > 1_000_000_000_000L) n else n * 1000L
                    }
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    /**
     * HistoryItem yang digabung (Real + Demo); urutan terbaru pertama.
     */
    suspend fun fetchAllMerged(context: Context, locale: String = "id"): Result<List<HistoryItem>> {
        val real = fetchTradeDeals(context, "real", locale).getOrElse { return Result.failure(it) }
        val demo = fetchTradeDeals(context, "demo", locale).getOrElse { return Result.failure(it) }
        val merged = (real + demo).sortedByDescending { it.createdAt }
        return Result.success(merged)
    }
}
