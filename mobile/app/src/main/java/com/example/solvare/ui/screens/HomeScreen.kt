package com.example.solvare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.solvare.nfc.NfcState
import com.example.solvare.ui.components.BalanceCard
import com.example.solvare.ui.components.NfcBottomSheet
import com.example.solvare.ui.components.SolWearBottomNav
import com.example.solvare.viewmodel.NfcPurpose
import com.example.solvare.viewmodel.WalletViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WalletViewModel,
    onNavigateToSend: () -> Unit,
    onTab: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val nfcState by viewModel.nfcState.collectAsState()
    val nfcErrorDetail by viewModel.nfcManager.lastErrorDetail.collectAsState()
    val activity = LocalContext.current as ComponentActivity

    DisposableEffect(activity) {
        viewModel.nfcManager.enableForegroundDispatch(activity)
        onDispose {
            viewModel.nfcManager.disableForegroundDispatch(activity)
        }
    }

    DisposableEffect(Unit) {
        viewModel.saveNavDestination("home")
        onDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Solvare",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    TextButton(onClick = { viewModel.onRefreshClicked() }) {
                        Text(
                            text = "Оновити",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = { SolWearBottomNav(current = "home", onSelect = onTab) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                BalanceCard(
                    balanceSol = uiState.balanceSol,
                    priceUsd = uiState.priceUsd,
                    isLoading = uiState.isLoadingBalance
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = formatLastConnection(uiState.lastConnectedAtMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (uiState.balanceError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.balanceError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.pubkey == null) {
                    Text(
                        text = "Натисніть кнопку оновлення,\nщоб підключити годинник",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    Text(
                        text = uiState.pubkey!!.take(8) + "..." + uiState.pubkey!!.takeLast(8),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onNavigateToSend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = uiState.pubkey != null && uiState.balanceSol != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Надіслати",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (uiState.showNfcSheet && uiState.nfcPurpose == NfcPurpose.READ_WALLET) {
                NfcBottomSheet(
                    nfcState = nfcState,
                    purpose = NfcPurpose.READ_WALLET,
                    onDismiss = { viewModel.onNfcSheetDismissed() },
                    errorDetail = nfcErrorDetail
                )
            }
        }
    }
}

private fun formatLastConnection(timestampMillis: Long?): String {
    if (timestampMillis == null) {
        return "Останнє підключення: -"
    }
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return "Останнє підключення: ${formatter.format(Date(timestampMillis))}"
}
