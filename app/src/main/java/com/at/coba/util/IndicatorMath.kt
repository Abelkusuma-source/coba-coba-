package com.at.coba.util

import com.at.coba.data.model.Candle
import kotlin.math.abs

object IndicatorMath {

    /**
     * Menghitung RSI (Relative Strength Index) secara efisien.
     * @param candles List candle (minimal period + 1)
     * @param period Periode RSI (default 14)
     * @return Nilai RSI (0-100) atau 50.0 jika data tidak cukup.
     */
    fun calculateRSI(candles: List<Candle>, period: Int = 14): Double {
        if (candles.size <= period) return 50.0

        var gain = 0.0
        var loss = 0.0

        // Hitung rata-rata awal
        for (i in 1..period) {
            val change = candles[i].close - candles[i - 1].close
            if (change > 0) gain += change else loss += abs(change)
        }

        var avgGain = gain / period
        var avgLoss = loss / period

        // Wilder's Smoothing
        for (i in (period + 1) until candles.size) {
            val change = candles[i].close - candles[i - 1].close
            val currentGain = if (change > 0) change else 0.0
            val currentLoss = if (change < 0) abs(change) else 0.0

            avgGain = (avgGain * (period - 1) + currentGain) / period
            avgLoss = (avgLoss * (period - 1) + currentLoss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    /**
     * Menghitung MACD (Moving Average Convergence Divergence).
     * @return Triple(MACD Line, Signal Line, Histogram)
     */
    fun calculateMACD(
        candles: List<Candle>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): Triple<Double, Double, Double> {
        if (candles.size < slowPeriod + signalPeriod) return Triple(0.0, 0.0, 0.0)

        val closes = candles.map { it.close }
        
        val emaFast = calculateEMA(closes, fastPeriod)
        val emaSlow = calculateEMA(closes, slowPeriod)
        
        val macdLine = emaFast.last() - emaSlow.last()
        
        // Signal Line adalah EMA dari MACD Line
        // Untuk efisiensi, kita butuh history MACD Line untuk menghitung EMA-nya
        val macdHistory = mutableListOf<Double>()
        for (i in (slowPeriod - 1) until closes.size) {
            val f = calculateEMA(closes.subList(0, i + 1), fastPeriod).last()
            val s = calculateEMA(closes.subList(0, i + 1), slowPeriod).last()
            macdHistory.add(f - s)
        }
        
        val signalLine = calculateEMA(macdHistory, signalPeriod).last()
        val histogram = macdLine - signalLine

        return Triple(macdLine, signalLine, histogram)
    }

    private fun calculateEMA(data: List<Double>, period: Int): List<Double> {
        if (data.isEmpty()) return emptyList()
        val ema = mutableListOf<Double>()
        val multiplier = 2.0 / (period + 1)

        // Dimulai dengan SMA untuk nilai pertama
        var currentEma = data.take(period).average()
        ema.add(currentEma)

        for (i in period until data.size) {
            currentEma = (data[i] - currentEma) * multiplier + currentEma
            ema.add(currentEma)
        }
        return ema
    }
}
