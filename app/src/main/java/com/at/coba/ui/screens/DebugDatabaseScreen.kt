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
import com.at.coba.data.TradingStrategy
import com.at.coba.data.local.BotDatabaseProvider
import com.at.coba.data.local.DatabaseProvider
import com.at.coba.util.BotDealPresentation

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

@Composable
fun DebugBotDatabaseScreen() {
    DebugBotDatabasePanel(modifier = Modifier.fillMaxSize())
}

@Composable
fun DebugBotDatabasePanel(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val botDb = remember(ctx) { BotDatabaseProvider.get(ctx.applicationContext) }
    val cobaDb = remember(ctx) { DatabaseProvider.get(ctx.applicationContext) }
    val botDeals by botDb.botDealDao().observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val serverDeals by cobaDb.tradeDealDao().observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val uuidIndex = remember(serverDeals) {
        BotDealPresentation.indexServerDealsByUuid(serverDeals)
    }

    LazyColumn(
        modifier = modifier.padding(16.dp),
    ) {
        item {
            Text(
                text = "bot_deals (${botDeals.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(botDeals, key = { it.id }) { deal ->
            val match = deal.serverDealUuid?.trim()?.takeIf { it.isNotEmpty() }
                ?.let { uuidIndex[it] }
            val strat = TradingStrategy.fromStorageKey(deal.strategy)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "${deal.ric} · ${deal.trend} · ${deal.dealType}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        "Strategy: ${strat.displayLabel}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Lifecycle: ${BotDealPresentation.debugLifecycleLabel(deal, match)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    match?.let {
                        Text(
                            "Settled (server): ${it.status} · profit=${it.profit} ${it.currency}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        "amount=${deal.amountMinor} · ${deal.durationSeconds}s",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    when (strat) {
                        TradingStrategy.MACD_RSI -> {
                            Text(
                                "RSI=${String.format("%.2f", deal.rsi)} · MACD=${String.format("%.4f", deal.macd)} · signal=${String.format("%.4f", deal.macdSignal)} · hist=${String.format("%.4f", deal.histogram)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TradingStrategy.BOLLINGER -> {
                            Text(
                                "Bollinger Bands",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            if (deal.bbUpper != null && deal.bbMiddle != null && deal.bbLower != null) {
                                Text(
                                    "upper=${String.format("%.4f", deal.bbUpper)} mid=${String.format("%.4f", deal.bbMiddle)} lower=${String.format("%.4f", deal.bbLower)}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(
                                "RSI (aux)=${String.format("%.2f", deal.rsi)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TradingStrategy.PRICE_ACTION -> {
                            Text(
                                "Price Action",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                deal.priceActionNote ?: "(no pattern)",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    val wsColor = when (deal.wsReplyStatus) {
                        "ok" -> MaterialTheme.colorScheme.primary
                        "error" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        "ws=${deal.wsReplyStatus} ref=${deal.wsRef ?: "-"} uuid=${deal.serverDealUuid ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = wsColor,
                    )
                    deal.wsReplyMessage?.let { msg ->
                        Text(
                            "err: $msg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        "createdAt=${deal.createdAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
