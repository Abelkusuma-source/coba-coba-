package com.at.coba.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.at.coba.data.DataStoreManager

@Composable
fun DebugScreen(dataStoreManager: DataStoreManager) {
    val allData by dataStoreManager.allData.collectAsState(initial = emptyMap())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "DataStore Debug Info", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

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
