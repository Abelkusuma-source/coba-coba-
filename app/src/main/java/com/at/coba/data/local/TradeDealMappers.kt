package com.at.coba.data.local

import com.at.coba.ui.screens.HistoryItem

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
)
