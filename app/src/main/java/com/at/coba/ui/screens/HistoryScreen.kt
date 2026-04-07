package com.at.coba.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class HistoryItem(val id: Int, val pair: String, val status: String, val wallet: String, val amount: String)

@OptIn(ExperimentalMaterial3Api::class)
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

    val filteredItems = historyItems.filter {
        (statusFilter == "All" || it.status == statusFilter) &&
        (pairFilter == "All" || it.pair == pairFilter) &&
        (walletFilter == "All" || it.wallet == walletFilter)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterDropdown("Status", listOf("All", "won", "lost", "standoff"), statusFilter) { statusFilter = it }
            FilterDropdown("Pair", listOf("All", "ASIA/X", "USD/USDT"), pairFilter) { pairFilter = it }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FilterDropdown("Wallet", listOf("All", "Real - IDR", "Demo - USD", "Real - USD"), walletFilter, modifier = Modifier.fillMaxWidth()) { walletFilter = it }
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredItems) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Pair: ${item.pair}", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Status: ${item.status}")
                        Text(text = "Wallet: ${item.wallet}")
                        Text(text = "Amount: ${item.amount}")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(label: String, options: List<String>, selectedOption: String, modifier: Modifier = Modifier, onOptionSelected: (String) -> Unit) {
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
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
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
