package com.at.coba.data.network

import com.at.coba.data.DataStoreManager
import okhttp3.Cookie
import okhttp3.HttpUrl

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
        val header = getServerCookiesForOkHttpOnly()
        if (header.isEmpty()) return emptyList()
        val host = url.host
        return header.split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { part ->
                val p = part.split('=', limit = 2)
                if (p.size != 2) return@mapNotNull null
                val name = p[0].trim()
                val value = p[1].trim()
                if (name.isEmpty()) return@mapNotNull null
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
}
