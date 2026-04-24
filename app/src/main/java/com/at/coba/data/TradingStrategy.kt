package com.at.coba.data

/**
 * Persisted trading strategy for the live signal engine.
 */
enum class TradingStrategy(val displayLabel: String, val storageKey: String) {
    MACD_RSI("MACD + RSI", "macd_rsi"),
    BOLLINGER("Bollinger Bands", "bollinger"),
    PRICE_ACTION("Price Action", "price_action");

    companion object {
        fun fromStorageKey(key: String?): TradingStrategy =
            entries.find { it.storageKey == key } ?: MACD_RSI
    }
}
