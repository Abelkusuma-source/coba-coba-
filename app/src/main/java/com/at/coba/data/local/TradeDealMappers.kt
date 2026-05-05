package com.at.coba.data.local

import com.at.coba.ui.screens.HistoryItem
import com.at.coba.ui.screens.HistoryRowSource

fun HistoryItem.toTradeDealEntity(): TradeDealEntity = TradeDealEntity(
    dealId = id,
    accountMode = accountMode,
    pair = pair,
    status = status,
    type = type,
    currency = currency,
    amount = amount,
    profit = profit,
    createdAt = createdAt,
    serverUuid = serverUuid,
)

fun TradeDealEntity.toHistoryItem(): HistoryItem = HistoryItem(
    id = dealId,
    pair = pair,
    status = status,
    type = type,
    accountMode = accountMode,
    currency = currency,
    amount = amount,
    profit = profit,
    createdAt = createdAt,
    serverUuid = serverUuid,
    source = HistoryRowSource.Server,
    botLocalId = null,
    botStrategyKey = null,
)
