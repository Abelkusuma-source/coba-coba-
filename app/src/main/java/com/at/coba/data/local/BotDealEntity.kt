package com.at.coba.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bot_deals")
data class BotDealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ric: String,
    val trend: String,
    val dealType: String,
    val amountMinor: Long,
    val durationSeconds: Int,
    val strategy: String,
    val rsi: Double,
    val macd: Double,
    val macdSignal: Double,
    val histogram: Double,
    val bbUpper: Double?,
    val bbMiddle: Double?,
    val bbLower: Double?,
    val priceActionNote: String?,
    /** Phoenix ref string used to correlate WS reply. */
    val wsRef: String?,
    /** Server-returned deal UUID on success; null when pending or failed. */
    val serverDealUuid: String?,
    /** `pending`, `ok`, `error` */
    val wsReplyStatus: String,
    /** Error message from WS reply, if any. */
    val wsReplyMessage: String?,
    val createdAt: Long,
)
