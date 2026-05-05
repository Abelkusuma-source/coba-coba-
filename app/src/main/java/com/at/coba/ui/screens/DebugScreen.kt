package com.at.coba.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.at.coba.data.DataStoreManager
import com.at.coba.ui.Screen

@Composable
fun DebugScreen(
    dataStoreManager: DataStoreManager,
    navController: NavController? = null,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("DataStore") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Room DB") },
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Bot DB") },
            )
        }
        when (selectedTab) {
            0 -> DataStoreDebugTab(dataStoreManager = dataStoreManager)
            1 -> Column(modifier = Modifier.fillMaxSize()) {
                navController?.let { nav ->
                    TextButton(
                        onClick = { nav.navigate(Screen.DebugDb.route) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Text("Buka layar penuh Room DB")
                    }
                }
                DebugDatabasePanel(modifier = Modifier.weight(1f))
            }
            2 -> Column(modifier = Modifier.fillMaxSize()) {
                navController?.let { nav ->
                    TextButton(
                        onClick = { nav.navigate(Screen.DebugBotDb.route) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Text("Buka layar penuh Bot DB")
                    }
                }
                DebugBotDatabasePanel(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DataStoreDebugTab(dataStoreManager: DataStoreManager) {
    val allData by dataStoreManager.allData.collectAsState(initial = emptyMap())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "DataStore Debug Info",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        LazyColumn {
            items(allData.toList()) { (key, value) ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Key: $key", style = MaterialTheme.typography.labelLarge)
                        Text(text = "Value: $value", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
