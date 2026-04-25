package com.at.coba.data.network

import android.content.Context
import com.at.coba.data.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

class AssetSocketManager(private val dataStoreManager: DataStoreManager) {

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionStatus = MutableStateFlow<WebSocketStatus>(WebSocketStatus.Disconnected)
    val connectionStatus: StateFlow<WebSocketStatus> = _connectionStatus.asStateFlow()

    private val _receivedMessage = MutableStateFlow<String?>(null)
    val receivedMessage: StateFlow<String?> = _receivedMessage.asStateFlow()

    private val _tickData = MutableStateFlow<AssetTick?>(null)
    val tickData: StateFlow<AssetTick?> = _tickData.asStateFlow()

    companion object {
        private const val AS_URL = "wss://as.stockity.id/"

        private val TIME_FIELD_PREFIXES = arrayOf(
            "\"time\":",
            "\"timestamp\":",
            "\"server_time\":",
            "\"ts\":",
            "\"t\":"
        )

        /** Values below this are treated as Unix seconds; otherwise milliseconds. */
        private const val MILLIS_THRESHOLD = 1_000_000_000_000L
    }

    fun connect(context: Context) {
        if (_connectionStatus.value is WebSocketStatus.Connected ||
            _connectionStatus.value is WebSocketStatus.Connecting) return

        _connectionStatus.value = WebSocketStatus.Connecting

        scope.launch {
            try {
                var deviceId = CookieManager.getDeviceId()
                if (deviceId.isEmpty()) {
                    deviceId = withContext(Dispatchers.IO) {
                        dataStoreManager.getOrCreateDeviceId()
                    }
                    CookieManager.setDeviceId(deviceId)
                }

                val request = CookieManager.applyStockitySocketRequestHeaders(
                    Request.Builder().url(AS_URL),
                    deviceId
                )
                    .header("Origin", "https://stockity.id")
                    .header("Referer", "https://stockity.id/")
                    .header("Accept", "*/*")
                    .header("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
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
            val serverMillis = parseServerTimeMillis(json) ?: System.currentTimeMillis()
            _tickData.value = AssetTick(rate, serverMillis)
        } catch (_: Exception) {
        }
    }

    private fun parseServerTimeMillis(json: String): Long? {
        for (prefix in TIME_FIELD_PREFIXES) {
            val idx = json.indexOf(prefix)
            if (idx == -1) continue
            val number = parseJsonNumber(json, idx + prefix.length) ?: continue
            return normalizeToUnixMillis(number)
        }
        return null
    }

    private fun parseJsonNumber(json: String, fromIndex: Int): Double? {
        var i = fromIndex
        while (i < json.length && json[i].isWhitespace()) i++
        if (i >= json.length) return null
        if (json[i] == '"') return null
        var j = i
        while (j < json.length && (json[j].isDigit() || json[j] == '.' || json[j] == '-' || json[j] == 'e' || json[j] == 'E' || json[j] == '+')) j++
        if (j == i) return null
        return json.substring(i, j).toDoubleOrNull()
    }

    private fun normalizeToUnixMillis(value: Double): Long {
        val integral = value.toLong()
        if (value != value.toLong().toDouble()) {
            return if (value < MILLIS_THRESHOLD) (value * 1000.0).toLong() else integral
        }
        return if (integral in 1 until MILLIS_THRESHOLD) integral * 1000L else integral
    }
}
