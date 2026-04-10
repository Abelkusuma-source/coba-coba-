package com.at.coba.ui.screens

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WebScreen() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                // WebViewClient standar tanpa override kosong yang tidak perlu
                webViewClient = WebViewClient()
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    // Menghapus databaseEnabled yang sudah usang (deprecated)
                }
                
                loadUrl("https://www.google.com")
            }
        },
        update = {
            // Blok update dikosongkan agar WebView tidak reload terus-menerus saat recompose
        }
    )
}
