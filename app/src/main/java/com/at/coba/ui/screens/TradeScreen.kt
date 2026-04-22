package com.at.coba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.at.coba.data.TradingConfig
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
    val tradingConfig by viewModel.tradingConfig.collectAsStateWithLifecycle()
    val rsiValue = indicatorState.rsi

    var showConfigSheet by remember { mutableStateOf(false) }

    // Placeholder states for Asset Pair and Strategy (UI only)
    var assetPair by remember { mutableStateOf("CRYPTO IDX") }
    var strategy by remember { mutableStateOf("MACD + RSI") }

    val isRunning = wsStatus !is WebSocketStatus.Disconnected || asStatus !is WebSocketStatus.Disconnected
    val scrollState = rememberScrollState()

    if (showConfigSheet) {
        TradingConfigSheet(
            config = tradingConfig,
            onDismiss = { showConfigSheet = false },
            onSave = { newConfig ->
                viewModel.updateConfig(newConfig)
                showConfigSheet = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trading Terminal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showConfigSheet = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. SYSTEM STATUS card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SYSTEM STATUS", 
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow(label = "Main WebSocket (WS):", status = wsStatus)
                StatusRow(label = "Asset Socket (AS):", status = asStatus)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. TIMEFRAME dropdown
        LabeledDropdown(
            label = "TIMEFRAME",
            options = listOf(
                5 to "5 Seconds",
                15 to "15 Seconds",
                30 to "30 Seconds",
                60 to "1 Minute",
                300 to "5 Minutes",
                900 to "15 Minutes",
                3600 to "1 Hour"
            ),
            selectedValue = selectedTF,
            onSelect = { viewModel.setTimeframe(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. ASSET PAIR dropdown
        LabeledDropdown(
            label = "ASSET PAIR",
            options = listOf(
                "CRYPTO IDX" to "CRYPTO IDX",
                "AUD / USD" to "AUD / USD",
                "ASIA / X" to "ASIA / X"
            ),
            selectedValue = assetPair,
            onSelect = { assetPair = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. STRATEGY dropdown
        LabeledDropdown(
            label = "STRATEGY",
            options = listOf(
                "MACD + RSI" to "MACD + RSI",
                "Bollinger Bands" to "Bollinger Bands",
                "Price Action" to "Price Action"
            ),
            selectedValue = strategy,
            onSelect = { strategy = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Chart area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            ProfessionalCandlestickChart(
                candles = candles,
                modifier = Modifier.fillMaxSize()
            )
            
            // Indicator Overlay
            SignalOverlay(tradeSignal = tradeSignal, rsiValue = rsiValue)

            // Price Overlay (Real-time)
            if (tickData != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = String.format("%.2f", tickData?.rate),
                        color = Color.Cyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. START/STOP button
        Button(
            onClick = {
                if (isRunning) viewModel.stopConnection()
                else viewModel.startConnection(context)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isRunning) "STOP TRADING ENGINE" else "START TRADING ENGINE",
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> LabeledDropdown(
    label: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.find { it.first == selectedValue }?.second ?: "Select..."

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = currentLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (value, title) ->
                    DropdownMenuItem(
                        text = { Text(title) },
                        onClick = {
                            onSelect(value)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingConfigSheet(
    config: TradingConfig,
    onDismiss: () -> Unit,
    onSave: (TradingConfig) -> Unit
) {
    var rsiPeriod by remember { mutableStateOf(config.rsiPeriod.toString()) }
    var macdFast by remember { mutableStateOf(config.macdFast.toString()) }
    var macdSlow by remember { mutableStateOf(config.macdSlow.toString()) }
    var macdSignal by remember { mutableStateOf(config.macdSignal.toString()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Indicator Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = rsiPeriod,
                onValueChange = { rsiPeriod = it },
                label = { Text("RSI Period") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = macdFast,
                    onValueChange = { macdFast = it },
                    label = { Text("MACD Fast") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = macdSlow,
                    onValueChange = { macdSlow = it },
                    label = { Text("MACD Slow") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            OutlinedTextField(
                value = macdSignal,
                onValueChange = { macdSignal = it },
                label = { Text("MACD Signal Period") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = {
                    val newConfig = TradingConfig(
                        rsiPeriod = rsiPeriod.toIntOrNull() ?: config.rsiPeriod,
                        macdFast = macdFast.toIntOrNull() ?: config.macdFast,
                        macdSlow = macdSlow.toIntOrNull() ?: config.macdSlow,
                        macdSignal = macdSignal.toIntOrNull() ?: config.macdSignal
                    )
                    onSave(newConfig)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("APPLY & RESET CHART")
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
                rsiValue > 65 -> Color.Red.copy(alpha = 0.8f)
                rsiValue < 35 -> Color.Green.copy(alpha = 0.8f)
                else -> Color.Yellow.copy(alpha = 0.8f)
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
            color = signalColor.copy(alpha = 0.8f),
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
            is WebSocketStatus.Error -> "Error" to Color.Red
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
