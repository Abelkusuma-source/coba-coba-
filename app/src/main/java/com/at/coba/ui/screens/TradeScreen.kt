package com.at.coba.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.at.coba.data.network.WebSocketStatus

private val Slate50 = Color(0xFFF8FAFC)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate500 = Color(0xFF64748B)
private val Slate700 = Color(0xFF334155)

private val Zinc800 = Color(0xFF27272A)
private val Zinc900 = Color(0xFF18181B)
private val Zinc950 = Color(0xFF09090B)
private val Zinc300 = Color(0xFFD4D4D8)
private val Zinc400 = Color(0xFFA1A1AA)

private val Emerald500 = Color(0xFF10B981)
private val Emerald700 = Color(0xFF047857)
private val Emerald100 = Color(0xFFD1FAE5)
private val Emerald400 = Color(0xFF34D399)

private val Indigo600 = Color(0xFF4F46E5)

@Composable
fun TradeScreen(viewModel: TradeViewModel) {
    val context = LocalContext.current
    val wsStatus by viewModel.wsStatus.collectAsStateWithLifecycle()
    val asStatus by viewModel.asStatus.collectAsStateWithLifecycle()
    val tickData by viewModel.tickData.collectAsStateWithLifecycle()

    val isRunning = wsStatus !is WebSocketStatus.Disconnected || asStatus !is WebSocketStatus.Disconnected

    var timeframe by remember { mutableStateOf<String?>(null) }
    var pair by remember { mutableStateOf("cryidx") }
    var strategy by remember { mutableStateOf("macdrsi") }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pageBg = if (isDark) Zinc950 else Slate50
    val cardBg = if (isDark) Zinc900 else Color.White
    val cardBorder = if (isDark) Zinc800 else Slate200
    val labelMuted = if (isDark) Zinc400 else Slate500
    val textPrimary = if (isDark) Zinc300 else Slate700
    val chartBorder = if (isDark) Zinc800 else Slate300
    val chartBg = if (isDark) Zinc900.copy(alpha = 0.5f) else Color(0xFFF1F5F9)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .padding(bottom = 96.dp)
        ) {
            SystemStatusCard(
                wsStatus = wsStatus,
                asStatus = asStatus,
                cardBg = cardBg,
                cardBorder = cardBorder,
                labelMuted = labelMuted,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(24.dp))

            SelectorsSection(
                timeframe = timeframe,
                onTimeframe = { timeframe = it },
                pair = pair,
                onPair = { pair = it },
                strategy = strategy,
                onStrategy = { strategy = it },
                labelMuted = labelMuted,
                textPrimary = textPrimary,
                fieldBg = cardBg,
                fieldBorder = cardBorder
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (tickData != null) {
                Text(
                    text = String.format("Z-CRY/IDX  %.2f", tickData?.rate),
                    style = MaterialTheme.typography.titleMedium,
                    color = Indigo600,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp)) {
                ChartPlaceholder(
                    borderColor = chartBorder,
                    backgroundColor = chartBg,
                    labelMuted = labelMuted
                )
            }
        }

        FloatingActionButton(
            onClick = {
                if (isRunning) viewModel.stopConnection()
                else viewModel.startConnection(context)
            },
            containerColor = Indigo600,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = if (isRunning) "Stop connection" else "Start connection",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SystemStatusCard(
    wsStatus: WebSocketStatus,
    asStatus: WebSocketStatus,
    cardBg: Color,
    cardBorder: Color,
    labelMuted: Color,
    isDark: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "System Status",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = labelMuted
            )
            StatusLine(label = "Trade Socket", status = wsStatus, isDark = isDark)
            StatusLine(label = "Asset Socket", status = asStatus, isDark = isDark)
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    status: WebSocketStatus,
    isDark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isDark) Zinc300 else Slate700
        )
        when (val s = status) {
            is WebSocketStatus.Connected -> ConnectedPill(isDark = isDark)
            is WebSocketStatus.Connecting -> StatusPill(
                text = "Connecting...",
                background = if (isDark) Color(0x33FBBF24) else Color(0xFFFFF3CD),
                content = Color(0xFFE6A200),
                pulse = true
            )
            is WebSocketStatus.Disconnected -> StatusPill(
                text = "Disconnected",
                background = if (isDark) Color(0x33A1A1AA) else Color(0xFFF1F5F9),
                content = if (isDark) Zinc400 else Slate500,
                pulse = false
            )
            is WebSocketStatus.Error -> StatusPill(
                text = s.message,
                background = if (isDark) Color(0x33F87171) else Color(0xFFFFE4E6),
                content = Color(0xFFDC2626),
                pulse = false
            )
        }
    }
}

@Composable
private fun ConnectedPill(isDark: Boolean) {
    val bg = if (isDark) Color(0x1A10B981) else Emerald100
    val fg = if (isDark) Emerald400 else Emerald700
    StatusPill(
        text = "Connected",
        background = bg,
        content = fg,
        pulse = true
    )
}

@Composable
private fun StatusPill(
    text: String,
    background: Color,
    content: Color,
    pulse: Boolean
) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val dotAlpha = if (pulse) {
        infinite.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot"
        ).value
    } else 1f

    Surface(
        color = background,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(dotAlpha)
                    .clip(CircleShape)
                    .background(
                        if (text == "Connected") Emerald500 else content
                    )
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorsSection(
    timeframe: String?,
    onTimeframe: (String?) -> Unit,
    pair: String,
    onPair: (String) -> Unit,
    strategy: String,
    onStrategy: (String) -> Unit,
    labelMuted: Color,
    textPrimary: Color,
    fieldBg: Color,
    fieldBorder: Color
) {
    val timeOptions = listOf(
        null to "Select Timeframe",
        "5" to "5 Seconds",
        "15" to "15 Seconds",
        "30" to "30 Seconds",
        "60" to "1 Minute",
        "300" to "5 Minutes"
    )
    val pairOptions = listOf(
        "cryidx" to "CRYPTO IDX",
        "audusd" to "AUD / USD",
        "asiax" to "ASIA / X"
    )
    val strategyOptions = listOf(
        "macdrsi" to "MACD + RSI",
        "bollinger" to "Bollinger Bands",
        "something" to "Price Action"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 600.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledDropdown(
                    label = "Timeframe",
                    options = timeOptions,
                    selectedValue = timeframe,
                    onSelect = { onTimeframe(it) },
                    labelMuted = labelMuted,
                    textPrimary = textPrimary,
                    fieldBg = fieldBg,
                    fieldBorder = fieldBorder,
                    modifier = Modifier.weight(1f)
                )
                LabeledDropdown(
                    label = "Asset Pair",
                    options = pairOptions,
                    selectedValue = pair,
                    onSelect = { onPair(it) },
                    labelMuted = labelMuted,
                    textPrimary = textPrimary,
                    fieldBg = fieldBg,
                    fieldBorder = fieldBorder,
                    modifier = Modifier.weight(1f)
                )
                LabeledDropdown(
                    label = "Strategy",
                    options = strategyOptions,
                    selectedValue = strategy,
                    onSelect = { onStrategy(it) },
                    labelMuted = labelMuted,
                    textPrimary = textPrimary,
                    fieldBg = fieldBg,
                    fieldBorder = fieldBorder,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledDropdown(
                    label = "Timeframe",
                    options = timeOptions,
                    selectedValue = timeframe,
                    onSelect = { onTimeframe(it) },
                    labelMuted = labelMuted,
                    textPrimary = textPrimary,
                    fieldBg = fieldBg,
                    fieldBorder = fieldBorder,
                    modifier = Modifier.fillMaxWidth()
                )
                LabeledDropdown(
                    label = "Asset Pair",
                    options = pairOptions,
                    selectedValue = pair,
                    onSelect = { onPair(it) },
                    labelMuted = labelMuted,
                    textPrimary = textPrimary,
                    fieldBg = fieldBg,
                    fieldBorder = fieldBorder,
                    modifier = Modifier.fillMaxWidth()
                )
                LabeledDropdown(
                    label = "Strategy",
                    options = strategyOptions,
                    selectedValue = strategy,
                    onSelect = { onStrategy(it) },
                    labelMuted = labelMuted,
                    textPrimary = textPrimary,
                    fieldBg = fieldBg,
                    fieldBorder = fieldBorder,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> LabeledDropdown(
    label: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    labelMuted: Color,
    textPrimary: Color,
    fieldBg: Color,
    fieldBorder: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.find { it.first == selectedValue }?.second.orEmpty()

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = labelMuted,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                shape = RoundedCornerShape(12.dp),
                color = fieldBg,
                border = BorderStroke(1.dp, fieldBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currentLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = labelMuted
                    )
                }
            }
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

@Composable
private fun ChartPlaceholder(
    borderColor: Color,
    backgroundColor: Color,
    labelMuted: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.BarChart,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = labelMuted.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Chart Visualization",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = labelMuted
            )
        }
    }
}
