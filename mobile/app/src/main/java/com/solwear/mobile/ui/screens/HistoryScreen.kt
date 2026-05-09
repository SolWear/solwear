package com.solwear.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solwear.mobile.ui.components.SolWearBottomNav
import com.solwear.mobile.viewmodel.WalletViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onTab: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val pubkey = uiState.pubkey
    val usd = if (uiState.balanceSol != null && uiState.priceUsd != null) {
        uiState.balanceSol!! * uiState.priceUsd!!
    } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallet", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        },
        bottomBar = { SolWearBottomNav(current = "wallet", onSelect = onTab) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                WalletSummary(
                    balance = uiState.balanceSol?.let { String.format(Locale.US, "%.4f SOL", it) } ?: "- SOL",
                    usd = usd?.let { String.format(Locale.US, "~$%.2f", it) } ?: "~$-",
                    pubkey = pubkey,
                    lastSync = formatLastSync(uiState.lastConnectedAtMillis),
                )
            }

            item {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            if (pubkey == null) {
                item { EmptyState("Tap SolWear over NFC to load wallet activity.") }
            } else {
                items(placeholderActivity()) { item ->
                    TransactionRow(item)
                }
            }
        }
    }
}

@Composable
private fun WalletSummary(balance: String, usd: String, pubkey: String?, lastSync: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(balance, style = MaterialTheme.typography.headlineMedium)
        Text(usd, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        DetailLine("Address", pubkey?.let { "${it.take(8)}...${it.takeLast(8)}" } ?: "Not connected")
        DetailLine("Last NFC sync", lastSync)
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class ActivityItem(
    val kind: String,
    val amount: String,
    val counterparty: String,
    val time: String,
    val status: String,
)

private fun placeholderActivity(): List<ActivityItem> = listOf(
    ActivityItem("Received", "+0.12 SOL", "7Xa9...qP2", "2m ago", "confirmed"),
    ActivityItem("Sent", "-0.05 SOL", "FzQk...LmT", "1h ago", "confirmed"),
    ActivityItem("Sent", "-0.01 SOL", "Dn3v...Wr8", "yesterday", "confirmed"),
)

@Composable
private fun TransactionRow(item: ActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.kind, style = MaterialTheme.typography.titleSmall)
            Text(item.counterparty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(item.amount, style = MaterialTheme.typography.titleSmall)
            Text("${item.time} - ${item.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyState(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatLastSync(timestampMillis: Long?): String {
    if (timestampMillis == null) return "-"
    return SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(timestampMillis))
}
