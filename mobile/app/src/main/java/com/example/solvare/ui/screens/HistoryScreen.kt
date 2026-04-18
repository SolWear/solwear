package com.example.solvare.ui.screens

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
import androidx.compose.ui.unit.dp
import com.example.solvare.ui.components.SolWearBottomNav
import com.example.solvare.viewmodel.WalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: WalletViewModel,
    onTab: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val pubkey = uiState.pubkey

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        },
        bottomBar = { SolWearBottomNav(current = "history", onSelect = onTab) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (pubkey == null) "No wallet connected" else "Recent on-chain activity",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            if (pubkey == null) {
                EmptyState("Connect your SolWear watch to load signatures.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(placeholderActivity()) { item ->
                        HistoryRow(item)
                    }
                }
            }
        }
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
    ActivityItem("Received", "+0.12 SOL", "7Xa9…qP2", "2m ago", "confirmed"),
    ActivityItem("Sent", "-0.05 SOL", "FzQk…LmT", "1h ago", "confirmed"),
    ActivityItem("Sent", "-0.01 SOL", "Dn3v…Wr8", "yesterday", "confirmed"),
)

@Composable
private fun HistoryRow(item: ActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.kind, style = MaterialTheme.typography.titleSmall)
            Text(
                item.counterparty,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(item.amount, style = MaterialTheme.typography.titleSmall)
            Text(
                "${item.time} · ${item.status}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            msg,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
