package com.at.coba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.at.coba.data.network.WebSocketStatus
import com.at.coba.ui.components.ProfessionalCandlestickChart

@Composable
fun TradeScreen(viewModel: TradeViewModel) {
    val context = LocalContext.current
    val wsStatus by viewModel.wsStatus.collectAsStateWithLifecycle()
    val asStatus by viewModel.asStatus.collectAsStateWithLifecycle()
    
    val tickData by viewModel.tickData.collectAsStateWithLifecycle()
    val candles by viewModel.candleHistory.collectAsStateWithLifecycle()
    val selectedTF by viewModel.selectedTimeframe.collectAsStateWithLifecycle()
    val tradeSignal by viewModel.tradeSignal.collectAsStateWithLifecycle(TradeSignal.SCANNING)
    val indicatorState by viewModel.indicatorState.collectAsStateWithLifecycle(IndicatorState())
    val rsiValue = indicatorState.rsi
    
    // Anggap sedang "Running" jika salah satu socket sedang Connecting atau Connected
    val isRunning = wsStatus !is WebSocketStatus.Disconnected || asStatus !is WebSocketStatus.Disconnected

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Timeframe Selector
        TimeframeSelector(
            selectedTF = selectedTF,
            onTFSelected = { viewModel.setTimeframe(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Connection Status", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                StatusRow(label = "Main WebSocket (WS):", status = wsStatus)
                StatusRow(label = "Asset Socket (AS):", status = asStatus)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Price Display (Real-time from AS)
        if (tickData != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Price: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = String.format("%.2f", tickData?.rate),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Candlestick Chart - Menggunakan weight(1f) agar fleksibel
        Box(modifier = Modifier.weight(1f)) {
            ProfessionalCandlestickChart(
                candles = candles,
                modifier = Modifier.fillMaxSize()
            )
            
            // Indicator Overlay
            SignalOverlay(tradeSignal = tradeSignal, rsiValue = rsiValue)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control Button
        Button(
            onClick = {
                if (isRunning) {
                    viewModel.stopConnection()
                } else {
                    viewModel.startConnection(context)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = if (isRunning) "STOP CONNECTION" else "START CONNECTION",
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeframeSelector(selectedTF: Int, onTFSelected: (Int) -> Unit) {
    val timeframes = listOf(
        5 to "5s", 15 to "15s", 30 to "30s", 60 to "1m",
        300 to "5m", 900 to "15m", 1800 to "30m", 3600 to "1h", 10800 to "3h", 86400 to "1d"
    )
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = timeframes.find { it.first == selectedTF }?.second ?: "Select TF"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = "Timeframe: $selectedLabel",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            shape = RoundedCornerShape(8.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            timeframes.forEach { (seconds, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onTFSelected(seconds)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun BoxScope.SignalOverlay(tradeSignal: TradeSignal, rsiValue: Double) {
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "RSI: ", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        Text(
            text = String.format("%.1f", rsiValue),
            color = when {
                rsiValue > 65 -> Color.Red.copy(alpha = 0.6f)
                rsiValue < 35 -> Color.Green.copy(alpha = 0.6f)
                else -> Color.Yellow.copy(alpha = 0.6f)
            },
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        val (signalText, signalColor) = when (tradeSignal) {
            TradeSignal.BUY -> "BUY" to Color.Green
            TradeSignal.SELL -> "SELL" to Color.Red
            TradeSignal.SCANNING -> "SCAN" to Color.White
        }
        
        Text(
            text = signalText,
            color = signalColor.copy(alpha = 0.5f),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp
        )
    }
}

@Composable
fun StatusRow(label: String, status: WebSocketStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        
        val (text, color) = when (status) {
            is WebSocketStatus.Connected -> "Connected" to Color(0xFF4CAF50)
            is WebSocketStatus.Connecting -> "Connecting..." to Color(0xFFFFC107)
            is WebSocketStatus.Disconnected -> "Disconnected" to Color.Gray
            is WebSocketStatus.Error -> "Error: ${status.message}" to Color.Red
        }
        
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text(
                text = text,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
