package com.at.coba.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.at.coba.data.DataStoreManager
import com.at.coba.data.TradingConfig
import com.at.coba.data.network.AssetSocketManager
import com.at.coba.data.network.WebSocketManager
import com.at.coba.data.network.WebSocketStatus
import com.at.coba.data.network.AssetTick
import com.at.coba.data.model.Candle
import com.at.coba.util.CandleManager
import com.at.coba.util.IndicatorMath
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
    val histogram: Double = 0.0
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
        if (candles.size < 26) {
            _tradeSignal.value = TradeSignal.SCANNING
            return
        }

        val config = tradingConfig.value

        // 1. Hitung RSI dengan config period
        val rsi = IndicatorMath.calculateRSI(candles, config.rsiPeriod)

        // 2. Hitung MACD dengan config parameters
        val (macd, signal, hist) = IndicatorMath.calculateMACD(
            candles,
            config.macdFast,
            config.macdSlow,
            config.macdSignal
        )
        
        // Update Indicator State
        _indicatorState.value = IndicatorState(
            rsi = rsi,
            macd = macd,
            signal = signal,
            histogram = hist
        )

        // 3. Logika Sinyal Sensitif (RSI 35/65 untuk timeframe rendah)
        when {
            rsi < 35.0 && macd > signal -> {
                _tradeSignal.value = TradeSignal.BUY
            }
            rsi > 65.0 && macd < signal -> {
                _tradeSignal.value = TradeSignal.SELL
            }
            else -> {
                _tradeSignal.value = TradeSignal.SCANNING
            }
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
