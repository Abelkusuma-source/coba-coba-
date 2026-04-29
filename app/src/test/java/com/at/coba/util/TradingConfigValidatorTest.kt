package com.at.coba.util

import com.at.coba.data.TradingStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TradingConfigValidatorTest {

    @Test
    fun macdRsi_rsiAtBounds_valid() {
        val r = TradingConfigValidator.validateTradingConfig(
            TradingStrategy.MACD_RSI,
            rsiPeriod = "2", macdFast = "2", macdSlow = "3", macdSignal = "2",
            bbPeriod = "ignored", bbStdDev = "ignored"
        )
        assertTrue(r is ValidationResult.Valid)
    }

    @Test
    fun macdRsi_rsiAboveMax_invalid() {
        val r = TradingConfigValidator.validateTradingConfig(
            TradingStrategy.MACD_RSI,
            rsiPeriod = "101", macdFast = "12", macdSlow = "26", macdSignal = "9",
            bbPeriod = "", bbStdDev = ""
        ) as? ValidationResult.Invalid
        assertNotNull(r)
        assertEquals(TradingConfigErrorCode.RsiOutOfRange, r!!.byField[TradingConfigField.RSI_PERIOD]?.code)
    }

    @Test
    fun macdRsi_rsiEmpty_invalidRequired() {
        val r = TradingConfigValidator.validateTradingConfig(
            TradingStrategy.MACD_RSI,
            rsiPeriod = " ", macdFast = "12", macdSlow = "26", macdSignal = "9",
            bbPeriod = "", bbStdDev = ""
        ) as? ValidationResult.Invalid
        assertNotNull(r)
        assertEquals(TradingConfigErrorCode.Required, r!!.byField[TradingConfigField.RSI_PERIOD]?.code)
    }

    @Test
    fun macdRsi_fastEqualSlow_invalid() {
        val r = TradingConfigValidator.validateTradingConfig(
            TradingStrategy.MACD_RSI,
            rsiPeriod = "14", macdFast = "12", macdSlow = "12", macdSignal = "9",
            bbPeriod = "", bbStdDev = ""
        ) as? ValidationResult.Invalid
        assertNotNull(r)
        assertEquals(TradingConfigErrorCode.MacdOrderInvalid, r!!.byField[TradingConfigField.MACD_FAST]?.code)
    }

    @Test
    fun macdRsi_fastAboveSlow_invalid() {
        val r = TradingConfigValidator.validateTradingConfig(
            TradingStrategy.MACD_RSI,
            rsiPeriod = "14", macdFast = "30", macdSlow = "12", macdSignal = "9",
            bbPeriod = "", bbStdDev = ""
        ) as? ValidationResult.Invalid
        assertNotNull(r)
        assertTrue(r!!.byField.containsKey(TradingConfigField.MACD_FAST))
        assertTrue(r.byField.containsKey(TradingConfigField.MACD_SLOW))
    }

    @Test
    fun bollinger_onlyBbParams_macdRsiIgnored() {
        val valid = TradingConfigValidator.validateTradingConfig(
            TradingStrategy.BOLLINGER,
            rsiPeriod = "", macdFast = "99", macdSlow = "1", macdSignal = "1",
            bbPeriod = "20", bbStdDev = "2.0"
        )
        assertTrue(valid is ValidationResult.Valid)
    }

    @Test
    fun bollinger_bbPeriodBelowMin() {
        val r = TradingConfigValidator.validateTradingConfig(
            TradingStrategy.BOLLINGER,
            rsiPeriod = "", macdFast = "x", macdSlow = "x", macdSignal = "x",
            bbPeriod = "4", bbStdDev = "2.0"
        ) as? ValidationResult.Invalid
        assertNotNull(r)
        assertEquals(TradingConfigErrorCode.BbPeriodOutOfRange, r!!.byField[TradingConfigField.BB_PERIOD]?.code)
    }

    @Test
    fun priceAction_alwaysValid() {
        val v = TradingConfigValidator.validateTradingConfig(
            TradingStrategy.PRICE_ACTION, "", "", "", "", "", ""
        )
        assertTrue(v is ValidationResult.Valid)
    }

    @Test
    fun bbStdEdge_valid_bollinger() {
        val low = TradingConfigValidator.validateTradingConfig(
            TradingStrategy.BOLLINGER, "", "", "", "", "20", "0.5"
        )
        val high = TradingConfigValidator.validateTradingConfig(
            TradingStrategy.BOLLINGER, "", "", "", "", "20", "4.0"
        )
        assertTrue(low is ValidationResult.Valid)
        assertTrue(high is ValidationResult.Valid)
    }

    @Test
    fun bbStdTooLow_invalid_bollinger() {
        val r = TradingConfigValidator.validateTradingConfig(
            TradingStrategy.BOLLINGER, "", "", "", "", "20", "0.4"
        ) as? ValidationResult.Invalid
        assertNotNull(r)
        assertEquals(TradingConfigErrorCode.BbStdOutOfRange, r!!.byField[TradingConfigField.BB_STDDEV]?.code)
    }

    @Test
    fun mergeMacdRsi_preservesBbFromBaseline() {
        val baseline = com.at.coba.data.TradingConfig(
            rsiPeriod = 9,
            macdFast = 1,
            macdSlow = 2,
            macdSignal = 3,
            bbPeriod = 99,
            bbStdDevMultiplier = 3.5f,
            strategy = TradingStrategy.MACD_RSI
        )
        val merged = TradingConfigValidator.mergeToTradingConfig(
            TradingStrategy.MACD_RSI,
            baseline,
            rsiPeriod = "14",
            macdFast = "12",
            macdSlow = "26",
            macdSignal = "9",
            bbPeriod = "5",
            bbStdDev = "0.5"
        )
        assertEquals(99, merged.bbPeriod)
        assertEquals(3.5f, merged.bbStdDevMultiplier)
    }

    @Test
    fun mergeBollinger_preservesRsiAndMacdFromBaseline() {
        val baseline = com.at.coba.data.TradingConfig(
            rsiPeriod = 21,
            macdFast = 12,
            macdSlow = 26,
            macdSignal = 9,
            bbPeriod = 1,
            bbStdDevMultiplier = 1f,
            strategy = TradingStrategy.BOLLINGER
        )
        val merged = TradingConfigValidator.mergeToTradingConfig(
            TradingStrategy.BOLLINGER,
            baseline,
            rsiPeriod = "99",
            macdFast = "99",
            macdSlow = "99",
            macdSignal = "99",
            bbPeriod = "20",
            bbStdDev = "2.0"
        )
        assertEquals(21, merged.rsiPeriod)
        assertEquals(12, merged.macdFast)
        assertEquals(26, merged.macdSlow)
        assertEquals(9, merged.macdSignal)
        assertEquals(20, merged.bbPeriod)
        assertEquals(2.0f, merged.bbStdDevMultiplier)
    }
}
