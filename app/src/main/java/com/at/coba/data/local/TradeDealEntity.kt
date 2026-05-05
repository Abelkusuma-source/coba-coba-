package com.at.coba.data.local

import androidx.room.Entity

@Entity(tableName = "trade_deals", primaryKeys = ["dealId", "accountMode"])
data class TradeDealEntity(
    val dealId: Long,
    val accountMode: String,
    val pair: String,
    val status: String,
    val type: String,
    val currency: String,
    val amount: Double,
    val profit: Double,
    val createdAt: Long,
    /** Server deal UUID kalau ada di payload history (untuk korelasi dengan `bot_deals`). */
    val serverUuid: String? = null,
)
