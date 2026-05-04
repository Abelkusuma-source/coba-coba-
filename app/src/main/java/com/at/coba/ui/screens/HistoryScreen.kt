package com.at.coba.ui.screens

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.at.coba.data.repository.AssetsRepository
import com.at.coba.R
import java.text.SimpleDateFormat
import java.util.*

// 2. Data History disesuaikan dengan structure data asli
data class HistoryItem(
    val id: Long,
    val pair: String,
    val status: String, // Won, Lost, Tie
    val type: String,   // BUY, SELL
    val accountMode: String, // Real, Demo
    val currency: String,    // IDR, USD
    val amount: Double,
    val profit: Double,
    val createdAt: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    var statusFilter by remember { mutableStateOf("All") }
    var pairFilter by remember { mutableStateOf("All") }
    var accountFilter by remember { mutableStateOf("All") }
    var selectedItem by remember { mutableStateOf<HistoryItem?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val historyItems by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isPullRefreshing by viewModel.isPullRefreshing.collectAsStateWithLifecycle()
    val loadError by viewModel.error.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    LaunchedEffect(accountFilter) {
        viewModel.load(accountFilter)
    }

    val assetPairRics by viewModel.assetPairRics.collectAsStateWithLifecycle()
    val historyLastSyncedMs by viewModel.historyLastSyncedAtEpochMs.collectAsStateWithLifecycle()
    val pairOptions = remember(assetPairRics, historyItems) {
        val merged = AssetsRepository.mergeSortedRicLists(
            assetPairRics,
            historyItems.map { it.pair }
        )
        listOf("All") + merged
    }

    val filteredItems = remember(statusFilter, pairFilter, accountFilter, historyItems) {
        historyItems.filter {
            val statusOk = statusFilter == "All" || it.status.equals(statusFilter, ignoreCase = true)
            val pairOk = pairFilter == "All" || it.pair == pairFilter
            val accountOk = accountFilter == "All" || it.accountMode == accountFilter
            statusOk && pairOk && accountOk
        }
    }

    /** Setelah tarik-segarkan selesai, scroll ke atas agar tidak tertinggal di scroll lama. */
    var previousPullRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isPullRefreshing) {
        if (previousPullRefreshing && !isPullRefreshing) {
            listState.animateScrollToItem(0)
        }
        previousPullRefreshing = isPullRefreshing
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(loadError) {
        val msg = loadError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg)
        viewModel.clearError()
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        SnackbarHost(hostState = snackbarHostState)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterDropdown(stringResource(R.string.status), listOf("All", "Won", "Lost", "Tie"), statusFilter, Modifier.weight(1f)) { statusFilter = it }
            PairFilterDropdownWithSearch(
                label = stringResource(R.string.pair),
                options = pairOptions,
                selectedOption = pairFilter,
                searchPlaceholder = stringResource(R.string.history_search_pair_placeholder),
                noResultsLabel = stringResource(R.string.history_search_pair_no_results),
                modifier = Modifier.weight(1f)
            ) {
                pairFilter = it
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        FilterDropdown(stringResource(R.string.account_mode), listOf("All", "Real", "Demo"), accountFilter, Modifier.fillMaxWidth()) { accountFilter = it }

        val historySyncedSdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
        val syncedAt = historyLastSyncedMs
        if (syncedAt != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.history_last_synced, historySyncedSdf.format(Date(syncedAt))),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading && historyItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        PullToRefreshBox(
            isRefreshing = isPullRefreshing,
            onRefresh = { viewModel.refreshFromPull() },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    HistoryCard(
                        item = item,
                        onClick = {
                            selectedItem = item
                            showBottomSheet = true
                        }
                    )
                }
            }
        }
    }

    if (showBottomSheet && selectedItem != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                selectedItem = null
            },
            sheetState = sheetState
        ) {
            OrderDetailBottomSheetContent(selectedItem!!)
        }
    }
}

@Composable
fun HistoryCard(item: HistoryItem, onClick: () -> Unit) {
    val statusColor = when (item.status.lowercase()) {
        "won" -> Color(0xFF4CAF50)
        "lost" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val typeColor = if (item.type == "BUY") Color(0xFF2196F3) else Color(0xFFE91E63)
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val formattedDate = sdf.format(Date(item.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.pair,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = formattedDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = item.status.uppercase(),
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(color = typeColor, shape = MaterialTheme.shapes.extraSmall)
                            .height(18.dp)
                            .width(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.type,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.None
                                )
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.accountMode,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Amount: ${formatCurrency(item.amount, item.currency)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${if (item.profit >= 0) "+" else ""}${formatCurrency(item.profit, item.currency)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
fun OrderDetailBottomSheetContent(item: HistoryItem) {
    val statusColor = when (item.status.lowercase()) {
        "won" -> Color(0xFF4CAF50)
        "lost" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val typeColor = if (item.type == "BUY") Color(0xFF2196F3) else Color(0xFFE91E63)
    val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.order_details),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(24.dp))

        DetailRow(stringResource(R.string.pair), item.pair, MaterialTheme.colorScheme.primary)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.order_type), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Box(
                modifier = Modifier
                    .background(color = typeColor, shape = MaterialTheme.shapes.extraSmall)
                    .height(20.dp)
                    .width(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.type,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None
                        )
                    )
                )
            }
        }

        DetailRow(stringResource(R.string.status), item.status.uppercase(), statusColor)
        DetailRow(stringResource(R.string.account_mode), item.accountMode)
        DetailRow(stringResource(R.string.date_time), sdf.format(Date(item.createdAt)))

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.total_profit_loss), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${if (item.profit >= 0) "+" else ""}${formatCurrency(item.profit, item.currency)}",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = statusColor
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(text = value, color = valueColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End, modifier = Modifier.weight(1.5f))
    }
}

fun formatCurrency(amount: Double, currency: String): String {
    return if (currency == "IDR") {
        val localeId = Locale.forLanguageTag("id-ID")
        val formatter = java.text.NumberFormat.getCurrencyInstance(localeId)
        formatter.format(amount).replace("Rp", "Rp ")
    } else {
        String.format(Locale.US, "$%.2f", amount)
    }
}

/** Exposed dropdown Pair dengan field pencarian di dalam menu untuk daftar panjang. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairFilterDropdownWithSearch(
    label: String,
    options: List<String>,
    selectedOption: String,
    searchPlaceholder: String,
    noResultsLabel: String,
    modifier: Modifier = Modifier,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(expanded) {
        if (!expanded) searchQuery = ""
    }

    val filteredOptions = remember(searchQuery, options) {
        val q = searchQuery.trim()
        if (q.isEmpty()) options
        else options.filter { it.contains(q, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text(searchPlaceholder, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* filter only; tetap dalam menu */ })
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

            if (filteredOptions.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(noResultsLabel, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = { },
                    enabled = false
                )
            } else {
                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(label: String, options: List<String>, selectedOption: String, modifier: Modifier = Modifier, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = selectedOption, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium, singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onOptionSelected(it); expanded = false }) }
        }
    }
}
