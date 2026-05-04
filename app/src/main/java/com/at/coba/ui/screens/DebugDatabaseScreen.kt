package com.at.coba.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.at.coba.data.local.DatabaseProvider

/**
 * Daftar isi Room: [trade_deals] dan [asset_choices].
 * Dipakai di tab Debug atau rute [com.at.coba.ui.Screen.DebugDb].
 */
@Composable
fun DebugDatabaseScreen() {
    DebugDatabasePanel(modifier = Modifier.fillMaxSize())
}

@Composable
fun DebugDatabasePanel(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val db = remember(ctx) { DatabaseProvider.get(ctx.applicationContext) }
    val tradeDeals by db.tradeDealDao().observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val assetChoices by db.assetChoiceDao().observeChoicesOrdered()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    LazyColumn(
        modifier = modifier.padding(16.dp),
    ) {
        item {
            Text(
                text = "trade_deals (${tradeDeals.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(tradeDeals, key = { "${it.dealId}_${it.accountMode}" }) { deal ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "id=${deal.dealId} · ${deal.accountMode} · ${deal.pair}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        "${deal.type} · ${deal.status} · ${deal.currency} · amount=${deal.amount} profit=${deal.profit}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "createdAt=${deal.createdAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Text(
                text = "asset_choices (${assetChoices.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
        }
        items(assetChoices, key = { it.ric }) { row ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(row.ric, style = MaterialTheme.typography.labelLarge)
                    Text(row.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
