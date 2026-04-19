package com.at.coba.data.network

import android.content.Context
import com.at.coba.data.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

sealed class WebSocketStatus {
    object Disconnected : WebSocketStatus()
    object Connecting : WebSocketStatus()
    object Connected : WebSocketStatus()
    data class Error(val message: String) : WebSocketStatus()
}

class WebSocketManager(private val dataStoreManager: DataStoreManager) {

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private var pingJob: Job? = null
    private var refCounter = 1

    private val _connectionStatus = MutableStateFlow<WebSocketStatus>(WebSocketStatus.Disconnected)
    val connectionStatus: StateFlow<WebSocketStatus> = _connectionStatus.asStateFlow()

    private val _receivedMessage = MutableStateFlow<String?>(null)
    val receivedMessage: StateFlow<String?> = _receivedMessage.asStateFlow()

    companion object {
        private const val WS_URL = "wss://ws.stockity.id/?v=2&vsn=2.0.0"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36"
        private const val HEARTBEAT_INTERVAL = 60_000L
        private const val PING_INTERVAL = 30_000L
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

                android.util.Log.d("WebSocketManager", "Final Cookie header: $cookieHeader")

                val requestBuilder = Request.Builder()
                    .url(WS_URL)
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
                        refCounter = 1
                        sendSubscribeMessages(webSocket)
                        startHeartbeat(webSocket)
                        startPing(webSocket)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        _receivedMessage.value = text
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        _connectionStatus.value = WebSocketStatus.Disconnected
                        stopTimers()
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        _connectionStatus.value = WebSocketStatus.Error(t.message ?: "Unknown error")
                        stopTimers()
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
        stopTimers()
    }

    private fun sendSubscribeMessages(webSocket: WebSocket) {
        val messages = listOf(
            """{"topic":"connection","event":"phx_join","payload":{},"ref":"${refCounter++}","join_ref":"1"}""",
            """{"topic":"bo","event":"phx_join","payload":{},"ref":"${refCounter++}","join_ref":"${refCounter - 1}"}""",
            """{"topic":"marathon","event":"phx_join","payload":{},"ref":"${refCounter++}","join_ref":"${refCounter - 1}"}""",
            """{"topic":"user","event":"phx_join","payload":{},"ref":"${refCounter++}","join_ref":"${refCounter - 1}"}""",
            """{"topic":"account","event":"phx_join","payload":{},"ref":"${refCounter++}","join_ref":"${refCounter - 1}"}""",
            """{"topic":"tournament","event":"phx_join","payload":{},"ref":"${refCounter++}","join_ref":"${refCounter - 1}"}""",
            """{"topic":"cfd_zero_spread","event":"phx_join","payload":{},"ref":"${refCounter++}","join_ref":"${refCounter - 1}"}""",
            """{"topic":"asset","event":"phx_join","payload":{},"ref":"${refCounter++}","join_ref":"${refCounter - 1}"}""",
            """{"topic":"copy_trading","event":"phx_join","payload":{},"ref":"${refCounter++}","join_ref":"${refCounter - 1}"}""",
            """{"topic":"asset:Z-CRY/IDX","event":"phx_join","payload":{},"ref":"${refCounter++}","join_ref":"${refCounter - 1}"}""",
            """{"topic":"range_stream:Z-CRY/IDX","event":"phx_join","payload":{},"ref":"${refCounter++}","join_ref":"${refCounter - 1}"}"""
        )
        messages.forEach { webSocket.send(it) }
    }

    private fun startHeartbeat(webSocket: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(HEARTBEAT_INTERVAL)
                if (_connectionStatus.value is WebSocketStatus.Connected) {
                    webSocket.send("""{"topic":"phoenix","event":"heartbeat","payload":{},"ref":"${refCounter++}"}""")
                }
            }
        }
    }

    private fun startPing(webSocket: WebSocket) {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (true) {
                delay(PING_INTERVAL)
                if (_connectionStatus.value is WebSocketStatus.Connected) {
                    webSocket.send("""{"topic":"connection","event":"ping","payload":{},"ref":"${refCounter++}","join_ref":"1"}""")
                }
            }
        }
    }

    private fun stopTimers() {
        heartbeatJob?.cancel()
        pingJob?.cancel()
        heartbeatJob = null
        pingJob = null
    }
}
