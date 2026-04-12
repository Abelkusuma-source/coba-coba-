package com.at.coba.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

data class HistoryItem(
    val id: Int,
    val pair: String,
    val status: String, // won, lost, standoff
    val orderType: String, // BUY, SELL
    val wallet: String, // e.g., "Real - IDR", "Demo - USD"
    val amount: Double,
    val profitLoss: Double,
    val timestamp: Long
)

@Composable
fun HistoryScreen() {
    var statusFilter by remember { mutableStateOf("All") }
    var pairFilter by remember { mutableStateOf("All") }
    var walletFilter by remember { mutableStateOf("All") }
    var selectedItem by remember { mutableStateOf<HistoryItem?>(null) }

    val historyItems = remember {
        listOf(
            HistoryItem(1, "ASIA/X", "won", "BUY", "Real - IDR", 100000.0, 85000.0, System.currentTimeMillis() - 3600000),
            HistoryItem(2, "USD/USDT", "lost", "SELL", "Demo - USD", 50.0, -50.0, System.currentTimeMillis() - 7200000),
            HistoryItem(3, "ASIA/X", "standoff", "BUY", "Real - USD", 100.0, 0.0, System.currentTimeMillis() - 10800000),
            HistoryItem(4, "USD/USDT", "won", "SELL", "Real - IDR", 200000.0, 170000.0, System.currentTimeMillis() - 14400000),
        )
    }

    val filteredItems = remember(statusFilter, pairFilter, walletFilter, historyItems) {
        historyItems.filter {
            (statusFilter == "All" || it.status == statusFilter) &&
            (pairFilter == "All" || it.pair == pairFilter) &&
            (walletFilter == "All" || it.wallet == walletFilter)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterDropdown(
                label = "Status",
                options = listOf("All", "won", "lost", "standoff"),
                selectedOption = statusFilter,
                modifier = Modifier.weight(1f)
            ) { statusFilter = it }

            FilterDropdown(
                label = "Pair",
                options = listOf("All", "ASIA/X", "USD/USDT"),
                selectedOption = pairFilter,
                modifier = Modifier.weight(1f)
            ) { pairFilter = it }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        FilterDropdown(
            label = "Wallet",
            options = listOf("All", "Real - IDR", "Demo - USD", "Real - USD"),
            selectedOption = walletFilter,
            modifier = Modifier.fillMaxWidth()
        ) { walletFilter = it }
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredItems, key = { it.id }) { item ->
                HistoryCard(item = item, onClick = { selectedItem = item })
            }
        }
    }

    // Detail Dialog
    selectedItem?.let { item ->
        OrderDetailDialog(item = item, onDismiss = { selectedItem = null })
    }
}

@Composable
fun HistoryCard(item: HistoryItem, onClick: () -> Unit) {
    val statusColor = when (item.status.lowercase()) {
        "won" -> Color(0xFF4CAF50)
        "lost" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val orderTypeColor = if (item.orderType == "BUY") Color(0xFF2196F3) else Color(0xFFE91E63)
    val walletIcon = if (item.wallet.contains("USD")) Icons.Default.AttachMoney else Icons.Default.Payments
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val formattedDate = sdf.format(Date(item.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Pair and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.pair,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Body: Order Type, Wallet, and Amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = orderTypeColor,
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.size(width = 40.dp, height = 18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = item.orderType,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = walletIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = item.wallet, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Amount: ${formatCurrency(item.amount, item.wallet)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${if (item.profitLoss >= 0) "+" else ""}${formatCurrency(item.profitLoss, item.wallet)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
fun OrderDetailDialog(item: HistoryItem, onDismiss: () -> Unit) {
    val statusColor = when (item.status.lowercase()) {
        "won" -> Color(0xFF4CAF50)
        "lost" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val orderTypeColor = if (item.orderType == "BUY") Color(0xFF2196F3) else Color(0xFFE91E63)
    val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.getDefault())
    val formattedDate = sdf.format(Date(item.timestamp))

    // Menentukan Mode Akun (Demo atau Real) berdasarkan isi wallet
    val accountMode = if (item.wallet.lowercase().contains("demo")) "Akun Demo" else "Akun Real"
    val modeColor = if (accountMode == "Akun Real") MaterialTheme.colorScheme.primary else Color(0xFFFFA000)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Order Details",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                DetailRow(label = "Pair", value = item.pair, valueColor = MaterialTheme.colorScheme.primary)
                DetailRow(label = "Order Type", value = item.orderType, valueColor = orderTypeColor)
                DetailRow(label = "Status", value = item.status.uppercase(), valueColor = statusColor)
                DetailRow(label = "Mode Akun", value = accountMode, valueColor = modeColor)
                DetailRow(label = "Tanggal & Waktu", value = formattedDate)

                Spacer(modifier = Modifier.height(24.dp))
                
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total Profit/Loss", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "${if (item.profitLoss >= 0) "+" else ""}${formatCurrency(item.profitLoss, item.wallet)}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = valueColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }
}

fun formatCurrency(amount: Double, wallet: String): String {
    return if (wallet.contains("IDR")) {
        val localeId = Locale.forLanguageTag("id-ID")
        val formatter = java.text.NumberFormat.getCurrencyInstance(localeId)
        formatter.format(amount).replace("Rp", "Rp ")
    } else {
        String.format(Locale.US, "$%.2f", amount)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    modifier: Modifier = Modifier,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onOptionSelected(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}
