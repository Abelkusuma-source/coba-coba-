package com.at.coba.util

import com.at.coba.data.model.Candle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CandleManager(private val maxLimit: Int = 100) {

    private val _candles = mutableListOf<Candle>()
    private val _candleFlow = MutableStateFlow<List<Candle>>(emptyList())
    val candleFlow = _candleFlow.asStateFlow()

    /**
     * Memproses tick harga baru menjadi candle OHLC
     */
    fun processTick(price: Double, serverTime: Long, timeframeSeconds: Int = 5) {
        val timeframeMs = timeframeSeconds * 1000L
        // Menentukan kapan candle ini dimulai (dibulatkan ke timeframe)
        val candleStartTime = (serverTime / timeframeMs) * timeframeMs

        synchronized(this) {
            val lastCandle = _candles.lastOrNull()

            if (lastCandle == null || lastCandle.timestamp != candleStartTime) {
                // Buat candle baru jika belum ada atau ganti periode
                val newCandle = Candle(
                    timestamp = candleStartTime,
                    open = price,
                    high = price,
                    low = price,
                    close = price
                )
                _candles.add(newCandle)
                
                // LIMIT RAM: Hanya simpan 100 terakhir
                if (_candles.size > maxLimit) {
                    _candles.removeAt(0)
                }
            } else {
                // Update candle yang sedang berjalan (Running Candle)
                lastCandle.close = price
                if (price > lastCandle.high) lastCandle.high = price
                if (price < lastCandle.low) lastCandle.low = price
            }

            // Emit data baru ke UI (copy list untuk memicu StateFlow update)
            _candleFlow.value = _candles.toList()
        }
    }
    
    fun clear() {
        synchronized(this) {
            _candles.clear()
            _candleFlow.value = emptyList()
        }
    }
}
