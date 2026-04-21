package com.at.coba.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.at.coba.data.model.Candle

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(300.dp)
) {
    Canvas(modifier = modifier) {
        if (candles.isEmpty()) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // 1. AGAR MENGALIR KE KIRI: Kita hanya ambil 30 candle terakhir
        // Ini memastikan candle tetap tebal dan bergeser saat data baru masuk
        val maxVisible = 30 
        val visibleCandles = candles.takeLast(maxVisible)
        
        // 2. Skala Y dinamis berdasarkan candle yang terlihat saja (Auto-Zoom)
        val rawMaxPrice = visibleCandles.maxOf { it.high }
        val rawMinPrice = visibleCandles.minOf { it.low }
        val rawPriceRange = (rawMaxPrice - rawMinPrice).coerceAtLeast(0.00000001)
        
        // Tambahkan padding 15% di atas (topBound) agar tidak menabrak teks indikator
        val maxPrice = rawMaxPrice + (rawPriceRange * 0.15)
        val priceRange = (maxPrice - rawMinPrice).coerceAtLeast(0.00000001)

        // 3. Lebar candle tetap (karena dibagi maxVisible yang konstan)
        val candleWidth = canvasWidth / maxVisible
        val bodyWidth = candleWidth * 0.8f

        visibleCandles.forEachIndexed { index, candle ->
            // Posisi X: Index 0 (tertua dari 30) di kiri, Index terakhir (terbaru) di kanan
            val x = index * candleWidth + (candleWidth - bodyWidth) / 2
            
            fun Double.toY(): Float {
                return ((maxPrice - this) / priceRange).toFloat() * canvasHeight
            }

            val openY = candle.open.toY()
            val closeY = candle.close.toY()
            val highY = candle.high.toY()
            val lowY = candle.low.toY()

            val isBullish = candle.close >= candle.open
            val color = if (isBullish) Color(0xFF00C853) else Color(0xFFFF1744)

            // Wick (Sumbu)
            drawLine(
                color = color,
                start = Offset(x + bodyWidth / 2, highY),
                end = Offset(x + bodyWidth / 2, lowY),
                strokeWidth = 2f
            )

            // Body
            val bodyHeight = Math.abs(openY - closeY).coerceAtLeast(1f)
            drawRect(
                color = color,
                topLeft = Offset(x, Math.min(openY, closeY)),
                size = Size(bodyWidth, bodyHeight)
            )
        }
    }
}

@Composable
fun ProfessionalCandlestickChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier
) {
    CandlestickChart(candles, modifier)
}
