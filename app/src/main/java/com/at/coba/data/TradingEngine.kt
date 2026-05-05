package com.at.coba.data

import android.app.Application
import android.content.Context
import com.at.coba.data.model.Candle
import com.at.coba.data.network.AssetSocketManager
import com.at.coba.data.network.WebSocketManager
import com.at.coba.data.repository.AssetChoice
import com.at.coba.util.CandleManager
import com.at.coba.util.IndicatorMath
import com.at.coba.util.PriceActionOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TradeSignal {
    data object BUY : TradeSignal()
    data object SELL : TradeSignal()
    data object SCANNING : TradeSignal()
}

data class IndicatorState(
    val rsi: Double = 50.0,
    val macd: Double = 0.0,
    val signal: Double = 0.0,
    val histogram: Double = 0.0,
    val bbUpper: Double? = null,
    val bbMiddle: Double? = null,
    val bbLower: Double? = null,
    val priceActionNote: String? = null,
)

/**
 * Satu instance aplikasi untuk socket BO + AS, candle, dan sinyal indikator.
 * Dipegang oleh [com.at.coba.service.TradingService] agar tetap hidup di background.
 */
class TradingEngine private constructor(
    private val dataStoreManager: DataStoreManager,
) {

    val webSocketManager = WebSocketManager(dataStoreManager)
    val assetSocketManager = AssetSocketManager(dataStoreManager)
    private val candleManager = CandleManager(100)

    private val engineJob = SupervisorJob()
    private val engineScope = CoroutineScope(engineJob + Dispatchers.Default)
    private var tickCollectorJob: Job? = null

    private val _selectedAsset = MutableStateFlow<AssetChoice?>(null)
    val selectedAsset: StateFlow<AssetChoice?> = _selectedAsset.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow(5)
    val selectedTimeframe: StateFlow<Int> = _selectedTimeframe.asStateFlow()

    private val _tradeSignal = MutableStateFlow<TradeSignal>(TradeSignal.SCANNING)
    val tradeSignal: StateFlow<TradeSignal> = _tradeSignal.asStateFlow()

    private val _indicatorState = MutableStateFlow(IndicatorState())
    val indicatorState: StateFlow<IndicatorState> = _indicatorState.asStateFlow()

    val tradingConfig: StateFlow<TradingConfig> = dataStoreManager.tradingConfig
        .stateIn(engineScope, SharingStarted.Eagerly, TradingConfig())

    val candleHistory: StateFlow<List<Candle>> = candleManager.candleFlow

    fun syncSelectionWithChoices(list: List<AssetChoice>) {
        if (list.isEmpty()) return
        val current = _selectedAsset.value
        when {
            current == null -> {
                val first = list.first()
                _selectedAsset.value = first
                assetSocketManager.setActiveRic(first.ric)
                webSocketManager.setAssetChannelRic(first.ric)
            }
            list.none { it.ric == current.ric } -> {
                val first = list.first()
                _selectedAsset.value = first
                assetSocketManager.setActiveRic(first.ric)
                webSocketManager.setAssetChannelRic(first.ric)
                candleManager.clear()
                _tradeSignal.value = TradeSignal.SCANNING
                _indicatorState.value = IndicatorState()
            }
            else -> {
                val updated = list.first { it.ric == current.ric }
                if (updated != current) {
                    _selectedAsset.value = updated
                }
            }
        }
    }

    fun selectAssetPair(choice: AssetChoice) {
        if (_selectedAsset.value?.ric == choice.ric) return
        _selectedAsset.value = choice
        assetSocketManager.setActiveRic(choice.ric)
        webSocketManager.setAssetChannelRic(choice.ric)
        candleManager.clear()
        _tradeSignal.value = TradeSignal.SCANNING
        _indicatorState.value = IndicatorState()
    }

    fun setTradingStrategy(strategy: TradingStrategy) {
        engineScope.launch {
            val current = tradingConfig.value
            if (current.strategy == strategy) return@launch
            dataStoreManager.updateTradingConfig(current.copy(strategy = strategy))
            candleManager.clear()
            _tradeSignal.value = TradeSignal.SCANNING
            _indicatorState.value = IndicatorState()
        }
    }

    fun changeTimeframe(seconds: Int) {
        val validSeconds = seconds.coerceIn(5, 86400)
        if (_selectedTimeframe.value != validSeconds) {
            _selectedTimeframe.value = validSeconds
            candleManager.clear()
            _tradeSignal.value = TradeSignal.SCANNING
            _indicatorState.value = IndicatorState()
        }
    }

    fun updateConfig(newConfig: TradingConfig) {
        engineScope.launch {
            dataStoreManager.updateTradingConfig(newConfig)
            candleManager.clear()
            _tradeSignal.value = TradeSignal.SCANNING
            _indicatorState.value = IndicatorState()
        }
    }

    fun sendBoTurboDeal(
        trend: String,
        amountDisplay: Double,
        durationSeconds: Int,
        dealType: String,
    ): Boolean {
        val ric = _selectedAsset.value?.ric ?: return false
        val amountMinor = (amountDisplay * BO_DEAL_AMOUNT_DISPLAY_SCALE).toLong().coerceAtLeast(1L)
        return webSocketManager.sendBoCreateDeal(
            ric = ric,
            trend = trend,
            amountMinor = amountMinor,
            dealType = dealType,
            durationSeconds = durationSeconds,
        )
    }

    /**
     * Hubungkan socket dan mulai proses tick → candle → sinyal.
     */
    fun startConnection(context: Context) {
        webSocketManager.connect(context)
        assetSocketManager.connect(context)
        startTickCollectionIfNeeded()
    }

    /**
     * Putus socket dan hentikan collector tick.
     */
    fun stopConnection() {
        tickCollectorJob?.cancel()
        tickCollectorJob = null
        webSocketManager.disconnect()
        assetSocketManager.disconnect()
    }

    /**
     * Reset chart/sinyal setelah logout (koneksi sudah di-stop terlebih dahulu).
     */
    fun clearTradingState() {
        candleManager.clear()
        _tradeSignal.value = TradeSignal.SCANNING
        _indicatorState.value = IndicatorState()
    }

    private fun startTickCollectionIfNeeded() {
        if (tickCollectorJob?.isActive == true) return
        tickCollectorJob = engineScope.launch {
            assetSocketManager.tickData.collect { tick ->
                tick?.let {
                    candleManager.processTick(
                        price = it.rate,
                        serverTime = it.time,
                        timeframeSeconds = _selectedTimeframe.value,
                    )
                    calculateSignals()
                }
            }
        }
    }

    private fun calculateSignals() {
        val candles = candleManager.candleFlow.value
        val config = tradingConfig.value
        when (config.strategy) {
            TradingStrategy.MACD_RSI -> applyMacdRsiStrategy(candles, config)
            TradingStrategy.BOLLINGER -> applyBollingerStrategy(candles, config)
            TradingStrategy.PRICE_ACTION -> applyPriceActionStrategy(candles)
        }
    }

    private fun applyMacdRsiStrategy(candles: List<Candle>, config: TradingConfig) {
        if (candles.size < config.macdSlow) {
            _tradeSignal.value = TradeSignal.SCANNING
            return
        }

        val rsi = IndicatorMath.calculateRSI(candles, config.rsiPeriod)
        val (macd, signal, hist) = IndicatorMath.calculateMACD(
            candles,
            config.macdFast,
            config.macdSlow,
            config.macdSignal,
        )

        _indicatorState.value = IndicatorState(
            rsi = rsi,
            macd = macd,
            signal = signal,
            histogram = hist,
            bbUpper = null,
            bbMiddle = null,
            bbLower = null,
            priceActionNote = null,
        )

        when {
            rsi < 35.0 && macd > signal -> _tradeSignal.value = TradeSignal.BUY
            rsi > 65.0 && macd < signal -> _tradeSignal.value = TradeSignal.SELL
            else -> _tradeSignal.value = TradeSignal.SCANNING
        }
    }

    private fun applyBollingerStrategy(candles: List<Candle>, config: TradingConfig) {
        val period = config.bbPeriod
        if (candles.size < period) {
            _tradeSignal.value = TradeSignal.SCANNING
            return
        }

        val bands = IndicatorMath.calculateBollingerBands(
            candles,
            period,
            config.bbStdDevMultiplier.toDouble(),
        ) ?: run {
            _tradeSignal.value = TradeSignal.SCANNING
            return
        }

        val close = candles.last().close
        val rsi = if (candles.size > config.rsiPeriod) {
            IndicatorMath.calculateRSI(candles, config.rsiPeriod)
        } else {
            50.0
        }

        _indicatorState.value = IndicatorState(
            rsi = rsi,
            macd = 0.0,
            signal = 0.0,
            histogram = 0.0,
            bbUpper = bands.upper,
            bbMiddle = bands.middle,
            bbLower = bands.lower,
            priceActionNote = null,
        )

        when {
            close < bands.lower -> _tradeSignal.value = TradeSignal.BUY
            close > bands.upper -> _tradeSignal.value = TradeSignal.SELL
            else -> _tradeSignal.value = TradeSignal.SCANNING
        }
    }

    private fun applyPriceActionStrategy(candles: List<Candle>) {
        if (candles.size < 3) {
            _tradeSignal.value = TradeSignal.SCANNING
            return
        }

        val outcome = IndicatorMath.evaluatePriceAction(candles)
        val note = when (outcome) {
            is PriceActionOutcome.Bullish -> outcome.pattern
            is PriceActionOutcome.Bearish -> outcome.pattern
            PriceActionOutcome.Neutral -> null
        }

        _indicatorState.value = IndicatorState(
            rsi = 50.0,
            macd = 0.0,
            signal = 0.0,
            histogram = 0.0,
            bbUpper = null,
            bbMiddle = null,
            bbLower = null,
            priceActionNote = note,
        )

        when (outcome) {
            is PriceActionOutcome.Bullish -> _tradeSignal.value = TradeSignal.BUY
            is PriceActionOutcome.Bearish -> _tradeSignal.value = TradeSignal.SELL
            PriceActionOutcome.Neutral -> _tradeSignal.value = TradeSignal.SCANNING
        }
    }

    companion object {
        private const val BO_DEAL_AMOUNT_DISPLAY_SCALE = 100.0

        @Volatile
        private var instance: TradingEngine? = null

        fun getInstance(application: Application, dataStoreManager: DataStoreManager): TradingEngine {
            return instance ?: synchronized(this) {
                instance ?: TradingEngine(dataStoreManager).also { instance = it }
            }
        }
    }
}
