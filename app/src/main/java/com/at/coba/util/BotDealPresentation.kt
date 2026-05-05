package com.at.coba.util

import com.at.coba.data.TradingStrategy
import com.at.coba.data.local.BotDealEntity
import com.at.coba.data.local.TradeDealEntity
import com.at.coba.ui.screens.HistoryItem
import com.at.coba.ui.screens.HistoryRowSource

object BotDealPresentation {

    private const val DISPLAY_SCALE = 100.0

    fun strategyDisplayLabel(storageKey: String): String =
        TradingStrategy.fromStorageKey(storageKey).displayLabel

    fun expireAtEpochMs(deal: BotDealEntity): Long =
        deal.createdAt + deal.durationSeconds.toLong() * 1000L

    /**
     * Normalisasi untuk filter History (won / lost / tie) + status bot lain (pending, failed, open, awaiting).
     */
    fun resolveBotStatus(deal: BotDealEntity, matchedServer: TradeDealEntity?): String = when {
        deal.wsReplyStatus == "pending" -> "pending"
        deal.wsReplyStatus == "error" -> "failed"
        matchedServer != null -> matchedServer.status.lowercase()
        deal.wsReplyStatus == "ok" && !deal.serverDealUuid.isNullOrBlank() -> {
            val now = System.currentTimeMillis()
            if (now < expireAtEpochMs(deal)) "open"
            else "awaiting"
        }
        else -> "pending"
    }

    /**
     * Label satu baris untuk debug Bot DB ("open", "won", "failed", …).
     */
    fun debugLifecycleLabel(deal: BotDealEntity, matchedServer: TradeDealEntity?): String {
        val raw = resolveBotStatus(deal, matchedServer)
        return when (raw) {
            "pending" -> "Pending (WS)"
            "failed" -> "Failed"
            "open" -> "Open"
            "awaiting" -> "Awaiting sync"
            "won" -> "Won"
            "lost", "lose" -> "Lost"
            "tie", "draw", "equal" -> "Tie"
            else -> raw.replaceFirstChar { it.titlecase() }
        }
    }

    fun botDealToHistoryItem(
        entity: BotDealEntity,
        matchedServer: TradeDealEntity?,
    ): HistoryItem {
        val status = resolveBotStatus(entity, matchedServer)
        val typeLabel = when (entity.trend.lowercase()) {
            "call", "buy", "up" -> "BUY"
            else -> "SELL"
        }
        val accountMode = when (entity.dealType.lowercase()) {
            "demo" -> "Demo"
            else -> "Real"
        }
        val amt = entity.amountMinor / DISPLAY_SCALE
        val profit = matchedServer?.profit ?: 0.0
        val cur = matchedServer?.currency ?: "IDR"
        val safeId = entity.id.takeIf { it > 0L } ?: 1L
        return HistoryItem(
            id = -safeId,
            pair = entity.ric,
            status = status,
            type = typeLabel,
            accountMode = accountMode,
            currency = cur,
            amount = amt,
            profit = profit,
            createdAt = entity.createdAt,
            serverUuid = entity.serverDealUuid,
            source = HistoryRowSource.Bot,
            botLocalId = entity.id.takeIf { it > 0L },
            botStrategyKey = entity.strategy,
        )
    }

    /** Map `trade_deals` yang punya UUID ke entri cepat untuk korelasi Bot. */
    fun indexServerDealsByUuid(entities: List<TradeDealEntity>): Map<String, TradeDealEntity> =
        entities.mapNotNull { e ->
            e.serverUuid?.trim()?.takeIf { it.isNotEmpty() }?.let { uuid -> uuid to e }
        }.toMap()
}
