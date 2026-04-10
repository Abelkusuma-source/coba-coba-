package com.at.coba.ui.screens

import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WebScreen() {
    val context = LocalContext.current
    val minVersion = "74.0.3279.185"

    // Mengecek versi Chrome menggunakan User Agent default sistem tanpa harus membuat WebView terlebih dahulu
    val versionInfo = remember(context) {
        val userAgent = WebSettings.getDefaultUserAgent(context)
        val currentVersion = extractChromeVersion(userAgent)
        val supported = isVersionGreaterOrEqual(currentVersion, minVersion)
        currentVersion to supported
    }

    val (chromeVersion, isSupported) = versionInfo

    if (!isSupported) {
        // Tampilan jika versi tidak didukung
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Update Required",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Chrome/WebView version ($chromeVersion) is too old.\nMinimum required: $minVersion",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        // Tampilan WebView jika versi memenuhi syarat
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    webViewClient = WebViewClient()
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }
                    
                    loadUrl("https://www.google.com")
                }
            },
            update = {} // Dikosongkan untuk menghindari reload saat recomposition
        )
    }
}

/**
 * Mengambil nomor versi dari string User Agent.
 */
private fun extractChromeVersion(userAgent: String): String {
    val regex = "Chrome/([\\d.]+)".toRegex()
    return regex.find(userAgent)?.groupValues?.get(1) ?: "0.0.0.0"
}

/**
 * Membandingkan versi saat ini dengan versi minimum.
 */
private fun isVersionGreaterOrEqual(current: String, minimum: String): Boolean {
    val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
    val minParts = minimum.split(".").map { it.toIntOrNull() ?: 0 }

    val maxLength = maxOf(currentParts.size, minParts.size)
    for (i in 0 until maxLength) {
        val curr = currentParts.getOrElse(i) { 0 }
        val min = minParts.getOrElse(i) { 0 }
        if (curr > min) return true
        if (curr < min) return false
    }
    return true
}
