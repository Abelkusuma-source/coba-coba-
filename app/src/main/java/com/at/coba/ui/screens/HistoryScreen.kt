package com.at.coba.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class HistoryItem(val id: Int, val pair: String, val status: String, val wallet: String, val amount: String)

@Composable
fun HistoryScreen() {
    var statusFilter by remember { mutableStateOf("All") }
    var pairFilter by remember { mutableStateOf("All") }
    var walletFilter by remember { mutableStateOf("All") }

    val historyItems = remember {
        listOf(
            HistoryItem(1, "ASIA/X", "won", "Real - IDR", "$100"),
            HistoryItem(2, "USD/USDT", "lost", "Demo - USD", "$50"),
            HistoryItem(3, "ASIA/X", "standoff", "Real - USD", "$0"),
            HistoryItem(4, "USD/USDT", "won", "Real - IDR", "$200"),
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredItems, key = { it.id }) { item ->
                HistoryCard(item)
            }
        }
    }
}

@Composable
fun HistoryCard(item: HistoryItem) {
    val statusColor = when (item.status.lowercase()) {
        "won" -> Color(0xFF4CAF50)
        "lost" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.pair, 
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.amount,
                    style = MaterialTheme.typography.titleLarge,
                    color = statusColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Status: ${item.status.uppercase()}", 
                    color = statusColor, 
                    style = MaterialTheme.typography.labelLarge
                )
                Text(text = item.wallet, style = MaterialTheme.typography.bodyMedium)
            }
        }
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true) // Menggunakan API baru
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
