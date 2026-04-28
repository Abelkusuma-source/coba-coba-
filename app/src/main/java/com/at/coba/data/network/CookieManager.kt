package com.at.coba.data.network

import com.at.coba.data.DataStoreManager
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.Request

/**
 * In-memory read-through cache for server cookies, device id, and auth token.
 * Updated from DataStore flows and from [applyServerCookiesFromHttpResponse] (OkHttp [CookieJar]).
 * Synchronous [getDeviceId], [getAuthToken], and [getCookieHeader] for network paths without
 * [kotlinx.coroutines.runBlocking].
 */
object CookieManager {

    private val lock = Any()
    @Volatile
    private var serverCookiesRaw: String = ""
    @Volatile
    private var deviceId: String = ""
    @Volatile
    private var authToken: String? = null

    fun setServerCookiesFromDataStore(value: String?) {
        val v = value.orEmpty()
        synchronized(lock) { serverCookiesRaw = v }
    }

    fun setDeviceId(id: String) {
        deviceId = id
    }

    fun setAuthToken(token: String?) {
        authToken = token
    }

    fun getDeviceId(): String = deviceId

    fun getAuthToken(): String? = authToken

    /** Trim; jalur private API mengharapkan token mentah di header `Authorization-Token` (tanpa prefix `Bearer `). */
    fun getDeviceIdForHeader(): String = deviceId.trim()

    fun getAuthorizationTokenForHeader(): String? {
        val t = authToken ?: return null
        var s = t.trim()
        if (s.startsWith("Bearer ", ignoreCase = true)) {
            s = s.substring(7).trim()
        }
        return s.takeIf { it.isNotEmpty() }
    }

    fun getServerCookiesForOkHttpOnly(): String = synchronized(lock) { serverCookiesRaw }

    /**
     * Merges [serverCookies] into the in-memory string (SESSION and others in one string),
     * returns the merged value to persist to DataStore.
     */
    fun applyServerCookiesFromHttpResponse(cookies: List<Cookie>): String {
        if (cookies.isEmpty()) {
            return synchronized(lock) { serverCookiesRaw }
        }
        return synchronized(lock) {
            val map = parseCookieStringToMap(serverCookiesRaw)
            for (c in cookies) {
                map[c.name] = c.value
            }
            serverCookiesRaw = mapToHeaderString(map)
            serverCookiesRaw
        }
    }

    /**
     * Merges raw `Set-Cookie` response lines into the in-memory jar and returns the merged header string.
     * Used when OkHttp's [CookieJar] receives an empty list (domain/path rules) but headers still carry session cookies.
     * Updates [serverCookiesRaw] synchronously so WebSocket connect right after login sees the same cookies.
     */
    fun persistSetCookieLines(url: HttpUrl, lines: List<String>): String {
        if (lines.isEmpty()) return getServerCookiesForOkHttpOnly()
        synchronized(lock) {
            val map = parseCookieStringToMap(serverCookiesRaw)
            for (line in lines) {
                val parsed = Cookie.parse(url, line)
                if (parsed != null) {
                    map[parsed.name] = parsed.value
                } else {
                    val segment = line.substringBefore(';').trim()
                    if (segment.isEmpty()) continue
                    val idx = segment.indexOf('=')
                    if (idx <= 0) continue
                    val name = segment.substring(0, idx).trim()
                    val value = segment.substring(idx + 1).trim()
                    if (name.isNotEmpty()) map[name] = value
                }
            }
            serverCookiesRaw = mapToHeaderString(map)
            return serverCookiesRaw
        }
    }

    /**
     * Full `Cookie` header: server cookies plus device_id, device_type, authtoken, and token
     * for parity with the previous WebSocket / interceptor behavior.
     */
    fun getCookieHeader(): String {
        val base = getServerCookiesForOkHttpOnly()
        val map: MutableMap<String, String> = parseCookieStringToMap(base)
        if (deviceId.isNotEmpty()) {
            map[DEVICE_ID_KEY] = deviceId
        }
        map[DEVICE_TYPE_KEY] = DataStoreManager.DEVICE_TYPE
        val t = authToken
        if (!t.isNullOrEmpty()) {
            map[AUTH_TOKEN_KEY] = t
            map[ALT_TOKEN_KEY] = t
        }
        return mapToHeaderString(map)
    }

    /**
     * OkHttp [CookieJar.loadForRequest] — in-memory only, no I/O.
     */
    fun cookiesForHttpUrl(url: HttpUrl): List<Cookie> {
        val host = url.host
        val header = getServerCookiesForOkHttpOnly()
        
        // Gabungkan Server Cookies dengan data identitas agar CookieJar mengirim lengkap
        val map = parseCookieStringToMap(header)
        if (deviceId.isNotEmpty()) {
            map[DEVICE_ID_KEY] = deviceId
        }
        map[DEVICE_TYPE_KEY] = DataStoreManager.DEVICE_TYPE
        
        val t = authToken
        if (!t.isNullOrEmpty()) {
            map[AUTH_TOKEN_KEY] = t
            map[ALT_TOKEN_KEY] = t
        }

        if (map.isEmpty()) return emptyList()

        return map.entries.mapNotNull { (name, value) ->
            try {
                Cookie.Builder()
                    .name(name)
                    .value(value)
                    .domain(host)
                    .build()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun mapToHeaderString(map: Map<String, String>): String =
        map.entries.joinToString("; ") { "${it.key}=${it.value}" }

    private fun parseCookieStringToMap(s: String): MutableMap<String, String> {
        if (s.isEmpty()) return mutableMapOf()
        val m = linkedMapOf<String, String>()
        s.split(';').forEach { part ->
            val p = part.trim()
            if (p.isEmpty()) return@forEach
            val kv = p.split('=', limit = 2)
            if (kv.size == 2) {
                val k = kv[0].trim()
                if (k.isNotEmpty()) m[k] = kv[1].trim()
            }
        }
        return m
    }

    private const val DEVICE_ID_KEY = "device_id"
    private const val DEVICE_TYPE_KEY = "device_type"
    private const val AUTH_TOKEN_KEY = "authtoken"
    private const val ALT_TOKEN_KEY = "token"

    /** Same UA string as [AuthInterceptor] for REST/Web parity. */
    const val STOCKITY_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    /**
     * Applies the same auth-related headers as API calls ([AuthInterceptor]): Cookie, Device-Id,
     * Device-Type, optional Authorization-Token, User-Agent.
     */
    fun applyStockitySocketRequestHeaders(builder: Request.Builder, deviceId: String): Request.Builder {
        var b = builder
            .header("Cookie", getCookieHeader())
            .header("Device-Id", deviceId.trim())
            .header("Device-Type", DataStoreManager.DEVICE_TYPE)
            .header("User-Agent", STOCKITY_USER_AGENT)
        getAuthorizationTokenForHeader()?.let { token ->
            b = b.header("Authorization-Token", token)
        }
        return b
    }
}
