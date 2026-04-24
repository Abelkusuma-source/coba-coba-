package com.at.coba.util

import com.at.coba.data.model.Candle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndicatorMathTest {

    @Test
    fun calculateBollingerBands_symmetricAroundFlatSeries() {
        val candles = (1..25).map { i ->
            Candle(timestamp = i.toLong(), open = 100.0, high = 100.0, low = 100.0, close = 100.0)
        }
        val bb = IndicatorMath.calculateBollingerBands(candles, period = 20, stdDevMultiplier = 2.0)
        assertNotNull(bb)
        assertEquals(100.0, bb!!.middle, 1e-9)
        assertEquals(100.0, bb.upper, 1e-9)
        assertEquals(100.0, bb.lower, 1e-9)
    }

    @Test
    fun calculateBollingerBands_insufficientData_returnsNull() {
        val candles = listOf(
            Candle(1, 1.0, 1.0, 1.0, 1.0),
            Candle(2, 2.0, 2.0, 2.0, 2.0)
        )
        val bb = IndicatorMath.calculateBollingerBands(candles, period = 20, stdDevMultiplier = 2.0)
        assertEquals(null, bb)
    }

    @Test
    fun evaluatePriceAction_bullishEngulfing() {
        val candles = listOf(
            Candle(1, 10.0, 11.0, 9.0, 10.5),
            Candle(2, 10.5, 10.8, 10.0, 10.2),
            Candle(3, 10.2, 11.5, 10.1, 11.4)
        )
        val o = IndicatorMath.evaluatePriceAction(candles)
        assertTrue(o is PriceActionOutcome.Bullish)
    }

    @Test
    fun evaluatePriceAction_neutralOnFlat() {
        val candles = listOf(
            Candle(1, 5.0, 5.0, 5.0, 5.0),
            Candle(2, 5.0, 5.0, 5.0, 5.0),
            Candle(3, 5.0, 5.0, 5.0, 5.0)
        )
        assertEquals(PriceActionOutcome.Neutral, IndicatorMath.evaluatePriceAction(candles))
    }
}
