package com.at.coba.util

import com.at.coba.data.TradingConfig
import com.at.coba.data.TradingStrategy

enum class TradingConfigField {
    RSI_PERIOD, MACD_FAST, MACD_SLOW, MACD_SIGNAL, BB_PERIOD, BB_STDDEV
}

enum class TradingConfigErrorCode {
    Required,
    NotAnInteger,
    NotADecimal,
    RsiOutOfRange,
    MacdFastOutOfRange,
    MacdSlowOutOfRange,
    MacdOrderInvalid,
    MacdSignalOutOfRange,
    BbPeriodOutOfRange,
    BbStdOutOfRange
}

data class FieldIssue(val code: TradingConfigErrorCode, val formatArgs: List<Any> = emptyList())

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val byField: Map<TradingConfigField, FieldIssue>) : ValidationResult()

    val isValid: Boolean
        get() = this is Valid
}

object TradingConfigValidator {
    const val RSI_MIN = 2
    const val RSI_MAX = 100
    const val MACD_FAST_MIN = 2
    const val MACD_FAST_MAX = 50
    const val MACD_SLOW_MIN = 3
    const val MACD_SLOW_MAX = 200
    const val MACD_SIGNAL_MIN = 2
    const val MACD_SIGNAL_MAX = 50
    const val BB_PERIOD_MIN = 5
    const val BB_PERIOD_MAX = 200
    const val BB_STD_MIN = 0.5
    const val BB_STD_MAX = 4.0

    /**
     * Pure validation for the trading config form. [strategy] controls which fields are required
     * (see strategy-aware UI).
     */
    fun validateTradingConfig(
        strategy: TradingStrategy,
        rsiPeriod: String,
        macdFast: String,
        macdSlow: String,
        macdSignal: String,
        bbPeriod: String,
        bbStdDev: String
    ): ValidationResult {
        val issues = linkedMapOf<TradingConfigField, FieldIssue>()

        fun add(field: TradingConfigField, issue: FieldIssue) {
            if (field !in issues) issues[field] = issue
        }

        when (strategy) {
            TradingStrategy.MACD_RSI -> {
                checkInt(
                    field = TradingConfigField.RSI_PERIOD, raw = rsiPeriod,
                    range = RSI_MIN..RSI_MAX, rangeCode = TradingConfigErrorCode.RsiOutOfRange,
                    issues = issues, add = ::add
                )
                checkInt(TradingConfigField.MACD_FAST, macdFast, MACD_FAST_MIN..MACD_FAST_MAX, TradingConfigErrorCode.MacdFastOutOfRange, issues, ::add)
                checkInt(TradingConfigField.MACD_SLOW, macdSlow, MACD_SLOW_MIN..MACD_SLOW_MAX, TradingConfigErrorCode.MacdSlowOutOfRange, issues, ::add)
                checkInt(TradingConfigField.MACD_SIGNAL, macdSignal, MACD_SIGNAL_MIN..MACD_SIGNAL_MAX, TradingConfigErrorCode.MacdSignalOutOfRange, issues, ::add)
                checkInt(TradingConfigField.BB_PERIOD, bbPeriod, BB_PERIOD_MIN..BB_PERIOD_MAX, TradingConfigErrorCode.BbPeriodOutOfRange, issues, ::add)
                checkFloat(TradingConfigField.BB_STDDEV, bbStdDev, issues, ::add)

                val f = macdFast.trim().toIntOrNull()
                val s = macdSlow.trim().toIntOrNull()
                if (f != null && s != null && f >= s) {
                    add(TradingConfigField.MACD_FAST, FieldIssue(TradingConfigErrorCode.MacdOrderInvalid, listOf(f, s)))
                    add(TradingConfigField.MACD_SLOW, FieldIssue(TradingConfigErrorCode.MacdOrderInvalid, listOf(f, s)))
                }
            }
            TradingStrategy.BOLLINGER -> {
                checkInt(TradingConfigField.RSI_PERIOD, rsiPeriod, RSI_MIN..RSI_MAX, TradingConfigErrorCode.RsiOutOfRange, issues, ::add)
                checkInt(TradingConfigField.BB_PERIOD, bbPeriod, BB_PERIOD_MIN..BB_PERIOD_MAX, TradingConfigErrorCode.BbPeriodOutOfRange, issues, ::add)
                checkFloat(TradingConfigField.BB_STDDEV, bbStdDev, issues, ::add)
            }
            TradingStrategy.PRICE_ACTION -> Unit
        }

        return if (issues.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(issues)
    }

    private fun checkInt(
        field: TradingConfigField,
        raw: String,
        range: IntRange,
        rangeCode: TradingConfigErrorCode,
        issues: MutableMap<TradingConfigField, FieldIssue>,
        add: (TradingConfigField, FieldIssue) -> Unit
    ) {
        val t = raw.trim()
        if (t.isEmpty()) {
            add(field, FieldIssue(TradingConfigErrorCode.Required))
            return
        }
        val v = t.toIntOrNull() ?: run {
            add(field, FieldIssue(TradingConfigErrorCode.NotAnInteger))
            return
        }
        if (v !in range) add(field, FieldIssue(rangeCode))
    }

    private fun checkFloat(
        field: TradingConfigField,
        raw: String,
        issues: MutableMap<TradingConfigField, FieldIssue>,
        add: (TradingConfigField, FieldIssue) -> Unit
    ) {
        val t = raw.trim()
        if (t.isEmpty()) {
            add(field, FieldIssue(TradingConfigErrorCode.Required))
            return
        }
        val v = t.toFloatOrNull()?.toDouble() ?: t.toDoubleOrNull() ?: run {
            add(field, FieldIssue(TradingConfigErrorCode.NotADecimal))
            return
        }
        if (v < BB_STD_MIN || v > BB_STD_MAX) add(field, FieldIssue(TradingConfigErrorCode.BbStdOutOfRange))
    }

    /**
     * Build persisted config after a successful [validateTradingConfig]. [baseline] holds MACD fields when
     * the active strategy does not show those inputs (e.g. Bollinger).
     */
    fun mergeToTradingConfig(
        strategy: TradingStrategy,
        baseline: TradingConfig,
        rsiPeriod: String,
        macdFast: String,
        macdSlow: String,
        macdSignal: String,
        bbPeriod: String,
        bbStdDev: String
    ): TradingConfig = when (strategy) {
        TradingStrategy.MACD_RSI -> TradingConfig(
            rsiPeriod = rsiPeriod.trim().toInt(),
            macdFast = macdFast.trim().toInt(),
            macdSlow = macdSlow.trim().toInt(),
            macdSignal = macdSignal.trim().toInt(),
            bbPeriod = bbPeriod.trim().toInt(),
            bbStdDevMultiplier = bbStdDev.trim().toFloat(),
            strategy = baseline.strategy
        )
        TradingStrategy.BOLLINGER -> TradingConfig(
            rsiPeriod = rsiPeriod.trim().toInt(),
            macdFast = baseline.macdFast,
            macdSlow = baseline.macdSlow,
            macdSignal = baseline.macdSignal,
            bbPeriod = bbPeriod.trim().toInt(),
            bbStdDevMultiplier = bbStdDev.trim().toFloat(),
            strategy = baseline.strategy
        )
        TradingStrategy.PRICE_ACTION -> TradingConfig(
            strategy = baseline.strategy
        )
    }
}
