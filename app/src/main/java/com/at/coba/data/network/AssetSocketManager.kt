package com.at.coba.data.network

import android.content.Context
import com.at.coba.data.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

data class AssetTick(
    val rate: Double,
    val time: Long
)

data class CandleData(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val time: Long
)

class AssetSocketManager(private val dataStoreManager: DataStoreManager) {

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionStatus = MutableStateFlow<WebSocketStatus>(WebSocketStatus.Disconnected)
    val connectionStatus: StateFlow<WebSocketStatus> = _connectionStatus.asStateFlow()

    private val _receivedMessage = MutableStateFlow<String?>(null)
    val receivedMessage: StateFlow<String?> = _receivedMessage.asStateFlow()

    private val _tickData = MutableStateFlow<AssetTick?>(null)
    val tickData: StateFlow<AssetTick?> = _tickData.asStateFlow()

    private val _candles = MutableStateFlow<List<CandleData>>(emptyList())
    val candles: StateFlow<List<CandleData>> = _candles.asStateFlow()

    private var currentCandle: CandleData? = null
    private var candlePeriodStart: Long = 0L

    companion object {
        private const val AS_URL = "wss://as.stockity.id/"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36"
        private const val MAX_CANDLES = 100
        private const val CANDLE_DURATION_MS = 5_000L // 5 seconds per candle
    }

    fun connect(context: Context) {
        if (_connectionStatus.value is WebSocketStatus.Connected ||
            _connectionStatus.value is WebSocketStatus.Connecting) return

        _connectionStatus.value = WebSocketStatus.Connecting

        scope.launch {
            try {
                val deviceId = dataStoreManager.getOrCreateDeviceId()
                val cookies = dataStoreManager.cookies.first()
                val authToken = dataStoreManager.authToken.first()
                val cookieHeader = buildString {
                    append("device_id=$deviceId; device_type=${DataStoreManager.DEVICE_TYPE}")
                    if (!cookies.isNullOrEmpty()) append("; $cookies")
                }

                android.util.Log.d("AssetSocketManager", "Final Cookie header: $cookieHeader")

                val requestBuilder = Request.Builder()
                    .url(AS_URL)
                    .addHeader("Device-Id", deviceId)
                    .addHeader("Device-Type", DataStoreManager.DEVICE_TYPE)
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Cookie", cookieHeader)

                if (!authToken.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization-Token", authToken)
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .build()

                webSocket = client.newWebSocket(requestBuilder.build(), object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        _connectionStatus.value = WebSocketStatus.Connected
                        sendSubscribeMessages(webSocket)
                        candlePeriodStart = System.currentTimeMillis()
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        _receivedMessage.value = text
                        parseAndBuildCandle(text)
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        _connectionStatus.value = WebSocketStatus.Disconnected
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        _connectionStatus.value = WebSocketStatus.Error(t.message ?: "Unknown error")
                    }
                })
            } catch (e: Exception) {
                _connectionStatus.value = WebSocketStatus.Error(e.message ?: "Connection failed")
            }
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionStatus.value = WebSocketStatus.Disconnected
        _receivedMessage.value = null
        currentCandle = null
    }

    private fun sendSubscribeMessages(webSocket: WebSocket) {
        webSocket.send("""{"action":"subscribe","event_type":"reconnect_request"}""")
        webSocket.send("""{"action":"subscribe","rics":["Z-CRY/IDX"]}""")
    }

    private fun parseAndBuildCandle(json: String) {
        try {
            val rateStart = json.indexOf("\"rate\":")
            if (rateStart == -1) return

            val rateValueStart = rateStart + 7
            val rateValueEnd = json.indexOf(",", rateValueStart).let {
                if (it == -1) json.indexOf("}", rateValueStart) else it
            }
            val rate = json.substring(rateValueStart, rateValueEnd).trim().toDoubleOrNull() ?: return

            val now = System.currentTimeMillis()
            _tickData.value = AssetTick(rate = rate, time = now)

            val candle = currentCandle
            if (candle == null) {
                currentCandle = CandleData(open = rate, high = rate, low = rate, close = rate, time = now)
                candlePeriodStart = now
            } else {
                currentCandle = candle.copy(
                    high = maxOf(candle.high, rate),
                    low = minOf(candle.low, rate),
                    close = rate
                )
            }

            if (now - candlePeriodStart >= CANDLE_DURATION_MS) {
                sealCandle()
            }
        } catch (e: Exception) {
            // Ignore parsing errors
        }
    }

    private fun sealCandle() {
        val sealed = currentCandle
        if (sealed != null) {
            val updatedList = (_candles.value + sealed).takeLast(MAX_CANDLES)
            _candles.value = updatedList
        }
        currentCandle = null
        candlePeriodStart = System.currentTimeMillis()
    }
}
