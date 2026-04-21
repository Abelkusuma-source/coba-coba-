package com.at.coba.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.at.coba.data.DataStoreManager
import com.at.coba.data.network.AssetSocketManager
import com.at.coba.data.network.WebSocketManager
import com.at.coba.data.network.WebSocketStatus
import com.at.coba.data.network.AssetTick
import com.at.coba.data.model.Candle
import com.at.coba.util.CandleManager
import com.at.coba.util.IndicatorMath
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class TradeSignal {
    object Neutral : TradeSignal()
    object Buy : TradeSignal()
    object Sell : TradeSignal()
}

class TradeViewModel(
    private val webSocketManager: WebSocketManager,
    private val assetSocketManager: AssetSocketManager
) : ViewModel() {

    // Timeframe aktif (5s, 10s, 15s, 30s, 60s)
    private val _selectedTimeframe = MutableStateFlow(5)
    val selectedTimeframe: StateFlow<Int> = _selectedTimeframe.asStateFlow()

    // Sinyal Trading (BUY/SELL/NEUTRAL)
    private val _tradeSignal = MutableStateFlow<TradeSignal>(TradeSignal.Neutral)
    val tradeSignal: StateFlow<TradeSignal> = _tradeSignal.asStateFlow()

    // Indikator Teknis untuk UI
    private val _rsiValue = MutableStateFlow(50.0)
    val rsiValue: StateFlow<Double> = _rsiValue.asStateFlow()

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
        if (candles.size < 15) return // Butuh data cukup untuk RSI(14)

        // 1. Hitung RSI
        val rsi = IndicatorMath.calculateRSI(candles)
        _rsiValue.value = rsi

        // 2. Hitung MACD
        val (macd, signal, _) = IndicatorMath.calculateMACD(candles)

        // 3. Logika Sinyal (Contoh Sederhana: RSI Oversold/Overbought + MACD Crossover)
        when {
            rsi < 30.0 && macd > signal -> {
                _tradeSignal.value = TradeSignal.Buy
            }
            rsi > 70.0 && macd < signal -> {
                _tradeSignal.value = TradeSignal.Sell
            }
            else -> {
                _tradeSignal.value = TradeSignal.Neutral
            }
        }
    }

    fun setTimeframe(seconds: Int) {
        if (_selectedTimeframe.value != seconds) {
            _selectedTimeframe.value = seconds
            candleManager.clear() // Reset chart saat timeframe ganti
            _tradeSignal.value = TradeSignal.Neutral
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
                    AssetSocketManager(dataStoreManager)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
