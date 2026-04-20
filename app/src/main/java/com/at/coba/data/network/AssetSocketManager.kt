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

// 1. Data Modeling: Membuat data class Candle yang efisien
data class Candle(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val timestamp: Long
)

data class AssetTick(
    val rate: Double,
    val time: Long
)

// 2. Logic Core: CandleProcessor untuk menghitung OHLC secara real-time
class CandleProcessor(private val durationMs: Long, private val maxCandles: Int) {
    private val _candles = MutableStateFlow<List<Candle>>(emptyList())
    val candles: StateFlow<List<Candle>> = _candles.asStateFlow()

    private var currentCandle: Candle? = null
    private var candlePeriodStart: Long = 0L
    private val lock = Any()

    // 5. Thread Safety: Memastikan pemrosesan data aman meskipun data datang sangat cepat
    fun processTick(rate: Double, time: Long) {
        synchronized(lock) {
            if (currentCandle == null || time - candlePeriodStart >= durationMs) {
                // Selesaikan candle lama jika ada
                currentCandle?.let { sealCandle(it) }
                // Mulai candle baru
                currentCandle = Candle(open = rate, high = rate, low = rate, close = rate, timestamp = time)
                candlePeriodStart = time
            } else {
                // Update candle berjalan (OHLC)
                currentCandle = currentCandle?.copy(
                    high = maxOf(currentCandle!!.high, rate),
                    low = minOf(currentCandle!!.low, rate),
                    close = rate
                )
            }
        }
    }

    private fun sealCandle(candle: Candle) {
        // 3. Memory Management: Implementasi sistem Fixed-Size Queue (Maksimal 100 candle)
        val currentList = _candles.value.toMutableList()
        currentList.add(candle)
        if (currentList.size > maxCandles) {
            currentList.removeAt(0)
        }
        _candles.value = currentList
    }

    fun reset() {
        synchronized(lock) {
            currentCandle = null
            candlePeriodStart = 0L
            _candles.value = emptyList()
        }
    }
}

class AssetSocketManager(private val dataStoreManager: DataStoreManager) {

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // 4. State Management: Menggunakan CandleProcessor yang mengekspos StateFlow
    private val candleProcessor = CandleProcessor(durationMs = 5_000L, maxCandles = 100)
    val candles: StateFlow<List<Candle>> = candleProcessor.candles

    private val _connectionStatus = MutableStateFlow<WebSocketStatus>(WebSocketStatus.Disconnected)
    val connectionStatus: StateFlow<WebSocketStatus> = _connectionStatus.asStateFlow()

    private val _receivedMessage = MutableStateFlow<String?>(null)
    val receivedMessage: StateFlow<String?> = _receivedMessage.asStateFlow()

    private val _tickData = MutableStateFlow<AssetTick?>(null)
    val tickData: StateFlow<AssetTick?> = _tickData.asStateFlow()

    companion object {
        private const val AS_URL = "wss://as.stockity.id/"
    }

    fun connect(context: Context) {
        if (_connectionStatus.value is WebSocketStatus.Connected ||
            _connectionStatus.value is WebSocketStatus.Connecting) return

        _connectionStatus.value = WebSocketStatus.Connecting

        scope.launch {
            try {
                val deviceId = dataStoreManager.getOrCreateDeviceId()
                val authToken = dataStoreManager.authToken.first()
                val cookies = dataStoreManager.cookies.first() ?: ""

                // Redundant/Robust Cookie logic untuk bypass 401
                val cookieMap = mutableMapOf<String, String>()
                if (cookies.isNotEmpty()) {
                    cookies.split(";").forEach {
                        val parts = it.split("=", limit = 2)
                        if (parts.size == 2) {
                            val key = parts[0].trim()
                            val value = parts[1].trim()
                            if (key.isNotEmpty()) cookieMap[key] = value
                        }
                    }
                }
                cookieMap["device_id"] = deviceId
                cookieMap["device_type"] = "web"
                if (!authToken.isNullOrEmpty()) {
                    cookieMap["authtoken"] = authToken
                    cookieMap["token"] = authToken
                }
                val finalCookieHeader = cookieMap.map { "${it.key}=${it.value}" }.joinToString("; ")

                val request = Request.Builder()
                    .url(AS_URL)
                    .header("Cookie", finalCookieHeader)
                    .header("Device-Id", deviceId)
                    .header("Device-Type", "web")
                    .header("Origin", "https://stockity.id")
                    .header("Referer", "https://stockity.id/")
                    .header("Accept", "*/*")
                    .header("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .build()

                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .build()

                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        _connectionStatus.value = WebSocketStatus.Connected
                        sendSubscribeMessages(webSocket)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        _receivedMessage.value = text
                        parseTick(text)
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
        candleProcessor.reset()
    }

    private fun sendSubscribeMessages(webSocket: WebSocket) {
        webSocket.send("""{"action":"subscribe","event_type":"reconnect_request"}""")
        webSocket.send("""{"action":"subscribe","rics":["Z-CRY/IDX"]}""")
    }

    private fun parseTick(json: String) {
        try {
            val rateStart = json.indexOf("\"rate\":")
            if (rateStart == -1) return
            val start = rateStart + 7
            var end = json.indexOf(",", start)
            if (end == -1) end = json.indexOf("}", start)
            if (end == -1) return
            
            val rate = json.substring(start, end).trim().toDoubleOrNull() ?: return
            val now = System.currentTimeMillis()

            // Update Tick Data (untuk UI harga instan)
            _tickData.value = AssetTick(rate, now)

            // Masukkan ke processor candle
            candleProcessor.processTick(rate, now)
        } catch (e: Exception) {}
    }
}
