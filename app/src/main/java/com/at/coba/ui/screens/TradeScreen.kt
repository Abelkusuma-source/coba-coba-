package com.at.coba.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.at.coba.R
import com.at.coba.data.repository.AssetChoice
import com.at.coba.data.IndicatorState
import com.at.coba.data.TradingConfig
import com.at.coba.data.TradingStrategy
import com.at.coba.data.TradeSignal
import com.at.coba.data.network.BoCreateDealResult
import com.at.coba.data.network.WebSocketStatus
import com.at.coba.ui.components.ProfessionalCandlestickChart
import com.at.coba.util.FieldIssue
import com.at.coba.util.TradingConfigErrorCode
import com.at.coba.util.TradingConfigField
import com.at.coba.util.TradingConfigValidator
import com.at.coba.util.ValidationResult
import kotlinx.coroutines.launch
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
    val assetChoices by viewModel.assetChoices.collectAsStateWithLifecycle()
    val selectedAsset by viewModel.selectedAsset.collectAsStateWithLifecycle()
    val isAssetsLoading by viewModel.isAssetsLoading.collectAsStateWithLifecycle()
    val assetsLoadError by viewModel.assetsLoadError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var dealAmountText by remember { mutableStateOf("10000") }
    var dealDurationSec by remember { mutableIntStateOf(30) }
    var dealIsDemo by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.boCreateResults.collect { result ->
            when (result) {
                is BoCreateDealResult.Ok -> {
                    val uuid = result.dealUuid ?: "—"
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.deal_order_ok, uuid),
                    )
                }
                is BoCreateDealResult.Error -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.deal_order_err, result.message),
                    )
                }
            }
        }
    }

    val isRunning = wsStatus !is WebSocketStatus.Disconnected || asStatus !is WebSocketStatus.Disconnected
    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(inner)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trading Terminal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
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

        // 3. ASSET PAIR (REST `/bo-assets/v6/assets` + pencarian di dropdown)
        SearchableAssetPairDropdown(
            choices = assetChoices,
            selected = selectedAsset,
            isLoading = isAssetsLoading,
            onSelect = { viewModel.selectAssetPair(it) }
        )
        assetsLoadError?.let { err ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { viewModel.retryLoadAssetChoices() }) {
                    Text("Coba lagi")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. STRATEGY dropdown (persisted; drives signal engine)
        LabeledDropdown(
            label = "STRATEGY",
            options = TradingStrategy.entries.map { it to it.displayLabel },
            selectedValue = tradingConfig.strategy,
            onSelect = { viewModel.setTradingStrategy(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4b. Konfigurasi indikator sesuai strategi (langsung di layar; sama validasi/simpan dengan sheet Pengaturan)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            TradingConfigStrategyParameters(
                config = tradingConfig,
                strategy = tradingConfig.strategy,
                snackbarHostState = snackbarHostState,
                onSave = { viewModel.updateConfig(it) },
                onDirtyChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                header = { onResetDefaults ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Konfigurasi indikator",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = onResetDefaults) {
                            Text(stringResource(R.string.trading_config_reset_to_defaults))
                        }
                    }
                }
            )
        }

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
            SignalOverlay(
                strategy = tradingConfig.strategy,
                indicatorState = indicatorState,
                tradeSignal = tradeSignal
            )

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

        // 5b. BO turbo order (Phoenix `bo` / `create`)
        val wsReady = wsStatus is WebSocketStatus.Connected
        val orderEnabled = wsReady && selectedAsset != null
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.deal_order_section),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dealAmountText,
                    onValueChange = { dealAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.deal_order_amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                LabeledDropdown(
                    label = stringResource(R.string.deal_order_duration_label),
                    options = listOf(30 to "30 s", 60 to "60 s", 120 to "120 s", 300 to "5 min"),
                    selectedValue = dealDurationSec,
                    onSelect = { dealDurationSec = it },
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = dealIsDemo,
                        onClick = { dealIsDemo = true },
                        label = { Text(stringResource(R.string.deal_order_mode_demo)) },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = !dealIsDemo,
                        onClick = { dealIsDemo = false },
                        label = { Text(stringResource(R.string.deal_order_mode_real)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!dealIsDemo) {
                    Text(
                        text = "Real: gunakan dengan hati-hati.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    fun submit(trend: String) {
                        if (!wsReady) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.deal_order_ws_required),
                                )
                            }
                            return
                        }
                        if (selectedAsset == null) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.deal_order_asset_required),
                                )
                            }
                            return
                        }
                        val amt = dealAmountText.toDoubleOrNull()
                        if (amt == null || amt <= 0.0) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.deal_order_amount_invalid),
                                )
                            }
                            return
                        }
                        val sent = viewModel.sendBoTurboDeal(
                            trend = trend,
                            amountDisplay = amt,
                            durationSeconds = dealDurationSec,
                            dealType = if (dealIsDemo) "demo" else "real",
                        )
                        if (!sent) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.deal_order_ws_required),
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { submit("call") },
                        enabled = orderEnabled,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                        ),
                    ) {
                        Text(stringResource(R.string.deal_order_buy), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { submit("put") },
                        enabled = orderEnabled,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC62828),
                        ),
                    ) {
                        Text(stringResource(R.string.deal_order_sell), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. START/STOP button
        Button(
            onClick = {
                if (isRunning) viewModel.stopConnection(context)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchableAssetPairDropdown(
    choices: List<AssetChoice>,
    selected: AssetChoice?,
    isLoading: Boolean,
    onSelect: (AssetChoice) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered by remember(choices, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) choices
            else choices.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                    it.ric.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(expanded) {
        if (!expanded) searchQuery = ""
    }

    LaunchedEffect(isLoading) {
        if (isLoading) expanded = false
    }

    val anchorText = when {
        isLoading -> "Memuat daftar aset…"
        selected != null -> selected.label
        else -> "Pilih pasangan aset"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "ASSET PAIR",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded && !isLoading,
            onExpandedChange = { if (!isLoading) expanded = it }
        ) {
            OutlinedTextField(
                value = anchorText,
                onValueChange = {},
                readOnly = true,
                enabled = !isLoading,
                trailingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .width(20.dp)
                                .height(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                modifier = Modifier
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = !isLoading)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            ExposedDropdownMenu(
                expanded = expanded && !isLoading,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    placeholder = { Text("Cari nama atau RIC") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                HorizontalDivider()
                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada hasil",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    filtered.forEach { choice ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    choice.label,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                onSelect(choice)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
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
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
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
private fun TradingConfigStrategyParameters(
    config: TradingConfig,
    strategy: TradingStrategy,
    snackbarHostState: SnackbarHostState,
    onSave: (TradingConfig) -> Unit,
    onDirtyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    header: @Composable (onResetDefaults: () -> Unit) -> Unit = { _ -> }
) {
    val context = LocalContext.current
    var rsiPeriod by remember(config) { mutableStateOf(config.rsiPeriod.toString()) }
    var macdFast by remember(config) { mutableStateOf(config.macdFast.toString()) }
    var macdSlow by remember(config) { mutableStateOf(config.macdSlow.toString()) }
    var macdSignal by remember(config) { mutableStateOf(config.macdSignal.toString()) }
    var bbPeriod by remember(config) { mutableStateOf(config.bbPeriod.toString()) }
    var bbStdDev by remember(config) { mutableStateOf(config.bbStdDevMultiplier.toString()) }

    val isDirty by remember(rsiPeriod, macdFast, macdSlow, macdSignal, bbPeriod, bbStdDev, config, strategy) {
        derivedStateOf {
            when (strategy) {
                TradingStrategy.MACD_RSI ->
                    rsiPeriod != config.rsiPeriod.toString() ||
                        macdFast != config.macdFast.toString() ||
                        macdSlow != config.macdSlow.toString() ||
                        macdSignal != config.macdSignal.toString()
                TradingStrategy.BOLLINGER ->
                    bbPeriod != config.bbPeriod.toString() ||
                        bbStdDev != config.bbStdDevMultiplier.toString()
                TradingStrategy.PRICE_ACTION -> false
            }
        }
    }

    LaunchedEffect(isDirty) {
        onDirtyChange(isDirty)
    }

    val scope = rememberCoroutineScope()
    val validation by remember(
        strategy,
        rsiPeriod,
        macdFast,
        macdSlow,
        macdSignal,
        bbPeriod,
        bbStdDev
    ) {
        derivedStateOf {
            TradingConfigValidator.validateTradingConfig(
                strategy, rsiPeriod, macdFast, macdSlow, macdSignal, bbPeriod, bbStdDev
            )
        }
    }
    val isFormValid by remember(validation) {
        derivedStateOf { validation.isValid }
    }
    val errorMap: Map<TradingConfigField, FieldIssue> =
        (validation as? ValidationResult.Invalid)?.byField.orEmpty()

    val fRsi = remember { FocusRequester() }
    val fMacdFast = remember { FocusRequester() }
    val fMacdSlow = remember { FocusRequester() }
    val fMacdSig = remember { FocusRequester() }
    val fBbP = remember { FocusRequester() }
    val fBbStd = remember { FocusRequester() }

    fun applyConfig() {
        if (!isFormValid) return
        onSave(
            TradingConfigValidator.mergeToTradingConfig(
                strategy, config, rsiPeriod, macdFast, macdSlow, macdSignal, bbPeriod, bbStdDev
            )
        )
    }

    fun resetFieldsToDefaults() {
        val d = TradingConfig().copy(strategy = config.strategy)
        rsiPeriod = d.rsiPeriod.toString()
        macdFast = d.macdFast.toString()
        macdSlow = d.macdSlow.toString()
        macdSignal = d.macdSignal.toString()
        bbPeriod = d.bbPeriod.toString()
        bbStdDev = d.bbStdDevMultiplier.toString()
        scope.launch {
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.trading_config_restored_snackbar)
            )
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = verticalArrangement
    ) {
        header(::resetFieldsToDefaults)

        if (strategy == TradingStrategy.PRICE_ACTION) {
            Text(
                text = stringResource(R.string.trading_config_price_action_note),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val muted = MaterialTheme.colorScheme.onSurfaceVariant
            val err = MaterialTheme.colorScheme.error

            when (strategy) {
                TradingStrategy.MACD_RSI -> {
                    OutlinedTextField(
                        value = rsiPeriod,
                        onValueChange = { rsiPeriod = filterInt(it) },
                        label = { Text(stringResource(R.string.trading_config_label_rsi)) },
                        isError = errorMap[TradingConfigField.RSI_PERIOD] != null,
                        supportingText = {
                            Column {
                                val i = errorMap[TradingConfigField.RSI_PERIOD]
                                if (i != null) {
                                    Text(
                                        text = tradingConfigIssueString(i, TradingConfigField.RSI_PERIOD),
                                        color = err
                                    )
                                } else {
                                    Text(
                                        stringResource(
                                            R.string.trading_config_range_rsi,
                                            TradingConfigValidator.RSI_MIN,
                                            TradingConfigValidator.RSI_MAX
                                        ),
                                        color = muted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(fRsi),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { fMacdFast.requestFocus() })
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = macdFast,
                                onValueChange = { macdFast = filterInt(it) },
                                label = { Text(stringResource(R.string.trading_config_label_macd_fast)) },
                                isError = errorMap[TradingConfigField.MACD_FAST] != null,
                                supportingText = {
                                    Column {
                                        val i = errorMap[TradingConfigField.MACD_FAST]
                                        if (i != null) {
                                            Text(
                                                text = tradingConfigIssueString(i, TradingConfigField.MACD_FAST),
                                                color = err
                                            )
                                        } else {
                                            val fI = macdFast.trim().toIntOrNull()
                                            val sI = macdSlow.trim().toIntOrNull()
                                            if (fI != null && sI != null && fI >= sI) {
                                                Text(
                                                    stringResource(
                                                        R.string.trading_config_macd_fast_slow_hint,
                                                        fI,
                                                        sI
                                                    ),
                                                    color = err
                                                )
                                            } else {
                                                Text(
                                                    stringResource(
                                                        R.string.trading_config_range_macd_fast,
                                                        TradingConfigValidator.MACD_FAST_MIN,
                                                        TradingConfigValidator.MACD_FAST_MAX
                                                    ),
                                                    color = muted,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(fMacdFast),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { fMacdSlow.requestFocus() })
                            )
                            OutlinedTextField(
                                value = macdSlow,
                                onValueChange = { macdSlow = filterInt(it) },
                                label = { Text(stringResource(R.string.trading_config_label_macd_slow)) },
                                isError = errorMap[TradingConfigField.MACD_SLOW] != null,
                                supportingText = {
                                    Column {
                                        val i = errorMap[TradingConfigField.MACD_SLOW]
                                        if (i != null) {
                                            Text(
                                                text = tradingConfigIssueString(i, TradingConfigField.MACD_SLOW),
                                                color = err
                                            )
                                        } else {
                                            Text(
                                                stringResource(
                                                    R.string.trading_config_range_macd_slow,
                                                    TradingConfigValidator.MACD_SLOW_MIN,
                                                    TradingConfigValidator.MACD_SLOW_MAX
                                                ),
                                                color = muted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(fMacdSlow),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { fMacdSig.requestFocus() })
                            )
                        }
                        OutlinedTextField(
                            value = macdSignal,
                            onValueChange = { macdSignal = filterInt(it) },
                            label = { Text(stringResource(R.string.trading_config_label_macd_signal)) },
                            isError = errorMap[TradingConfigField.MACD_SIGNAL] != null,
                            supportingText = {
                                val i = errorMap[TradingConfigField.MACD_SIGNAL]
                                if (i != null) {
                                    Text(
                                        tradingConfigIssueString(i, TradingConfigField.MACD_SIGNAL),
                                        color = err
                                    )
                                } else {
                                    Text(
                                        stringResource(
                                            R.string.trading_config_range_macd_signal,
                                            TradingConfigValidator.MACD_SIGNAL_MIN,
                                            TradingConfigValidator.MACD_SIGNAL_MAX
                                        ),
                                        color = muted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(fMacdSig),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { if (isFormValid) applyConfig() })
                        )
                    }
                }
                TradingStrategy.BOLLINGER -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.trading_config_bollinger_section),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = bbPeriod,
                                onValueChange = { bbPeriod = filterInt(it) },
                                label = { Text(stringResource(R.string.trading_config_label_bb_period)) },
                                isError = errorMap[TradingConfigField.BB_PERIOD] != null,
                                supportingText = {
                                    val e = errorMap[TradingConfigField.BB_PERIOD]
                                    if (e != null) {
                                        Text(
                                            tradingConfigIssueString(e, TradingConfigField.BB_PERIOD),
                                            color = err
                                        )
                                    } else {
                                        Text(
                                            stringResource(
                                                R.string.trading_config_range_bb_period,
                                                TradingConfigValidator.BB_PERIOD_MIN,
                                                TradingConfigValidator.BB_PERIOD_MAX
                                            ),
                                            color = muted,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(fBbP),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { fBbStd.requestFocus() })
                            )
                            OutlinedTextField(
                                value = bbStdDev,
                                onValueChange = { bbStdDev = filterDecimal(it) },
                                label = { Text(stringResource(R.string.trading_config_label_bb_std)) },
                                isError = errorMap[TradingConfigField.BB_STDDEV] != null,
                                supportingText = {
                                    val e = errorMap[TradingConfigField.BB_STDDEV]
                                    if (e != null) {
                                        Text(
                                            tradingConfigIssueString(e, TradingConfigField.BB_STDDEV),
                                            color = err
                                        )
                                    } else {
                                        Text(
                                            stringResource(
                                                R.string.trading_config_range_bb_std,
                                                TradingConfigValidator.BB_STD_MIN,
                                                TradingConfigValidator.BB_STD_MAX
                                            ),
                                            color = muted,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(fBbStd),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { if (isFormValid) applyConfig() })
                            )
                        }
                    }
                }
                TradingStrategy.PRICE_ACTION -> Unit
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { applyConfig() },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.trading_config_apply))
        }
    }
}

@Composable
private fun tradingConfigIssueString(issue: FieldIssue, field: TradingConfigField): String = when (issue.code) {
    TradingConfigErrorCode.Required -> if (field == TradingConfigField.RSI_PERIOD) {
        stringResource(R.string.trading_config_rsi_required)
    } else {
        stringResource(R.string.trading_config_error_required)
    }
    TradingConfigErrorCode.NotAnInteger -> stringResource(R.string.trading_config_error_not_integer)
    TradingConfigErrorCode.NotADecimal -> stringResource(R.string.trading_config_error_not_decimal)
    TradingConfigErrorCode.RsiOutOfRange -> stringResource(
        R.string.trading_config_error_rsi_range,
        TradingConfigValidator.RSI_MIN,
        TradingConfigValidator.RSI_MAX
    )
    TradingConfigErrorCode.MacdFastOutOfRange -> stringResource(
        R.string.trading_config_error_macd_fast_range,
        TradingConfigValidator.MACD_FAST_MIN,
        TradingConfigValidator.MACD_FAST_MAX
    )
    TradingConfigErrorCode.MacdSlowOutOfRange -> stringResource(
        R.string.trading_config_error_macd_slow_range,
        TradingConfigValidator.MACD_SLOW_MIN,
        TradingConfigValidator.MACD_SLOW_MAX
    )
    TradingConfigErrorCode.MacdOrderInvalid -> {
        val a = issue.formatArgs
        if (a.size >= 2) {
            stringResource(
                R.string.trading_config_error_macd_order,
                a[0] as Int,
                a[1] as Int
            )
        } else {
            stringResource(R.string.trading_config_error_not_integer)
        }
    }
    TradingConfigErrorCode.MacdSignalOutOfRange -> stringResource(
        R.string.trading_config_error_macd_signal_range,
        TradingConfigValidator.MACD_SIGNAL_MIN,
        TradingConfigValidator.MACD_SIGNAL_MAX
    )
    TradingConfigErrorCode.BbPeriodOutOfRange -> stringResource(
        R.string.trading_config_error_bb_period_range,
        TradingConfigValidator.BB_PERIOD_MIN,
        TradingConfigValidator.BB_PERIOD_MAX
    )
    TradingConfigErrorCode.BbStdOutOfRange -> stringResource(
        R.string.trading_config_error_bb_std_range,
        TradingConfigValidator.BB_STD_MIN,
        TradingConfigValidator.BB_STD_MAX
    )
}

private fun filterInt(s: String): String = s.filter { it.isDigit() }

private fun filterDecimal(s: String): String = buildString {
    var dot = false
    for (c in s) {
        when {
            c.isDigit() -> append(c)
            c == '.' && !dot -> {
                dot = true
                append('.')
            }
        }
    }
}

@Composable
fun BoxScope.SignalOverlay(
    strategy: TradingStrategy,
    indicatorState: IndicatorState,
    tradeSignal: TradeSignal
) {
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (strategy) {
            TradingStrategy.MACD_RSI -> {
                val rsiValue = indicatorState.rsi
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
            }
            TradingStrategy.BOLLINGER -> {
                val u = indicatorState.bbUpper
                val m = indicatorState.bbMiddle
                val l = indicatorState.bbLower
                Text(
                    text = if (u != null && m != null && l != null) {
                        String.format("BB %.2f / %.2f / %.2f", u, m, l)
                    } else {
                        "BB …"
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 200.dp)
                )
            }
            TradingStrategy.PRICE_ACTION -> {
                Text(
                    text = "PA: ${indicatorState.priceActionNote ?: "—"}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 160.dp)
                )
            }
        }

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
