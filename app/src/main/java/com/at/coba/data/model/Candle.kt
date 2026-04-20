package com.at.coba.data.model

data class Candle(
    val timestamp: Long, // Waktu buka candle (ms)
    val open: Double,
    var high: Double,
    var low: Double,
    var close: Double,
)
