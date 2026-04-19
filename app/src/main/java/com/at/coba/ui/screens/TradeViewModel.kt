package com.at.coba.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.at.coba.data.DataStoreManager
import com.at.coba.data.network.AssetSocketManager
import com.at.coba.data.network.WebSocketManager
import com.at.coba.data.network.WebSocketStatus
import com.at.coba.data.network.AssetTick
import com.at.coba.data.network.CandleData
import kotlinx.coroutines.flow.StateFlow

class TradeViewModel(
    private val webSocketManager: WebSocketManager,
    private val assetSocketManager: AssetSocketManager
) : ViewModel() {

    // Status Koneksi
    val wsStatus: StateFlow<WebSocketStatus> = webSocketManager.connectionStatus
    val asStatus: StateFlow<WebSocketStatus> = assetSocketManager.connectionStatus

    // Data dari WebSocket Utama (WS)
    val wsReceivedMessage: StateFlow<String?> = webSocketManager.receivedMessage

    // Data dari Asset Socket (AS)
    val asReceivedMessage: StateFlow<String?> = assetSocketManager.receivedMessage
    val tickData: StateFlow<AssetTick?> = assetSocketManager.tickData
    val candles: StateFlow<List<CandleData>> = assetSocketManager.candles

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
