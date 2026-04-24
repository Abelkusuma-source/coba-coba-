package com.at.coba.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.at.coba.data.DataStoreManager
import com.at.coba.data.TradingConfig
import com.at.coba.data.TradingStrategy
import com.at.coba.data.network.AssetSocketManager
import com.at.coba.data.network.WebSocketManager
import com.at.coba.data.network.WebSocketStatus
import com.at.coba.data.network.AssetTick
import com.at.coba.data.model.Candle
import com.at.coba.util.CandleManager
import com.at.coba.util.IndicatorMath
import com.at.coba.util.PriceActionOutcome
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TradeSignal {
    object BUY : TradeSignal()
    object SELL : TradeSignal()
    object SCANNING : TradeSignal()
}

data class IndicatorState(
    val rsi: Double = 50.0,
    val macd: Double = 0.0,
    val signal: Double = 0.0,
    val histogram: Double = 0.0,
    val bbUpper: Double? = null,
    val bbMiddle: Double? = null,
    val bbLower: Double? = null,
    val priceActionNote: String? = null
)

class TradeViewModel(
    private val webSocketManager: WebSocketManager,
    private val assetSocketManager: AssetSocketManager,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    // Timeframe aktif (5s, 10s, 15s, 30s, 60s)
    private val _selectedTimeframe = MutableStateFlow(5)
    val selectedTimeframe: StateFlow<Int> = _selectedTimeframe.asStateFlow()

    // Sinyal Trading (BUY/SELL/SCANNING)
    private val _tradeSignal = MutableStateFlow<TradeSignal>(TradeSignal.SCANNING)
    val tradeSignal: StateFlow<TradeSignal> = _tradeSignal.asStateFlow()

    // State Indikator untuk UI
    private val _indicatorState = MutableStateFlow(IndicatorState())
    val indicatorState: StateFlow<IndicatorState> = _indicatorState.asStateFlow()

    // Trading Configuration (RSI/MACD Periods)
    val tradingConfig: StateFlow<TradingConfig> = dataStoreManager.tradingConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TradingConfig()
        )

    // Tahap 3: Candle Management
    private val candleManager = CandleManager(100)
    val candleHistory: StateFlow<List<Candle>> = candleManager.candleFlow

    // Status Koneksi
    val wsStatus: StateFlow<WebSocketStatus> = webSocketManager.connectionStatus
    val asStatus: StateFlow<WebSocketStatus> = assetSocketManager.connectionStatus

    // Data dari WebSocket Utama (WS)
    val wsReceivedMessage: StateFlow<String?> = webSocketManager.receivedMessage

    // Data dari Asset Socket (AS)
    val asReceivedMessage: StateFlow<String?> = assetSocketManager.receivedMessage
    val tickData: StateFlow<AssetTick?> = assetSocketManager.tickData

    init {
        // Observasi tickData untuk mengupdate candleHistory secara real-time
        viewModelScope.launch {
            assetSocketManager.tickData.collect { tick ->
                tick?.let {
                    val currentTF = _selectedTimeframe.value
                    candleManager.processTick(
                        price = it.rate,
                        serverTime = it.time,
                        timeframeSeconds = currentTF
                    )
                    
                    // Hitung Indikator & Sinyal
                    calculateSignals()
                }
            }
        }
    }

    private fun calculateSignals() {
        val candles = candleHistory.value
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
            config.macdSignal
        )

        _indicatorState.value = IndicatorState(
            rsi = rsi,
            macd = macd,
            signal = signal,
            histogram = hist,
            bbUpper = null,
            bbMiddle = null,
            bbLower = null,
            priceActionNote = null
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
            config.bbStdDevMultiplier.toDouble()
        ) ?: run {
            _tradeSignal.value = TradeSignal.SCANNING
            return
        }

        val close = candles.last().close
        val rsi = if (candles.size > config.rsiPeriod) {
            IndicatorMath.calculateRSI(candles, config.rsiPeriod)
        } else 50.0

        _indicatorState.value = IndicatorState(
            rsi = rsi,
            macd = 0.0,
            signal = 0.0,
            histogram = 0.0,
            bbUpper = bands.upper,
            bbMiddle = bands.middle,
            bbLower = bands.lower,
            priceActionNote = null
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
            priceActionNote = note
        )

        when (outcome) {
            is PriceActionOutcome.Bullish -> _tradeSignal.value = TradeSignal.BUY
            is PriceActionOutcome.Bearish -> _tradeSignal.value = TradeSignal.SELL
            PriceActionOutcome.Neutral -> _tradeSignal.value = TradeSignal.SCANNING
        }
    }

    fun setTradingStrategy(strategy: TradingStrategy) {
        viewModelScope.launch {
            val current = tradingConfig.value
            if (current.strategy == strategy) return@launch
            dataStoreManager.updateTradingConfig(current.copy(strategy = strategy))
            candleManager.clear()
            _tradeSignal.value = TradeSignal.SCANNING
            _indicatorState.value = IndicatorState()
        }
    }

    fun setTimeframe(seconds: Int) {
        changeTimeframe(seconds)
    }

    fun changeTimeframe(seconds: Int) {
        // Validasi durasi (5s hingga 86400s/1 hari)
        val validSeconds = seconds.coerceIn(5, 86400)
        
        if (_selectedTimeframe.value != validSeconds) {
            _selectedTimeframe.value = validSeconds
            candleManager.clear() // Reset chart saat timeframe ganti
            _tradeSignal.value = TradeSignal.SCANNING
            _indicatorState.value = IndicatorState()
        }
    }

    /**
     * Update trading configuration and refresh indicators
     */
    fun updateConfig(newConfig: TradingConfig) {
        viewModelScope.launch {
            dataStoreManager.updateTradingConfig(newConfig)
            candleManager.clear() // Reset data agar indikator menghitung ulang dengan parameter baru
            _tradeSignal.value = TradeSignal.SCANNING
            _indicatorState.value = IndicatorState()
        }
    }

    /**
     * Menghubungkan kedua socket sekaligus
     */
    fun startConnection(context: Context) {
        webSocketManager.connect(context)
        assetSocketManager.connect(context)
    }

    /**
     * Memutuskan kedua koneksi socket
     */
    fun stopConnection() {
        webSocketManager.disconnect()
        assetSocketManager.disconnect()
    }

    /**
     * Melakukan proses logout secara aman
     */
    fun performLogout() {
        viewModelScope.launch {
            // 1. Stop koneksi socket
            stopConnection()
            
            // 2. Clear data candle
            candleManager.clear()
            
            // 3. Reset state indicators & signals
            _tradeSignal.value = TradeSignal.SCANNING
            _indicatorState.value = IndicatorState()
            
            // 4. Hapus data autentikasi dari DataStore
            // (Tetap mempertahankan Device ID sesuai requirement)
            dataStoreManager.clearAuthData()
        }
    }

    /**
     * Otomatis disconnect saat ViewModel dihancurkan
     */
    override fun onCleared() {
        super.onCleared()
        stopConnection()
    }

    class Factory(
        private val dataStoreManager: DataStoreManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TradeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TradeViewModel(
                    WebSocketManager(dataStoreManager),
                    AssetSocketManager(dataStoreManager),
                    dataStoreManager
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
