package com.example.solvare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.solvare.nfc.NfcMode
import com.example.solvare.nfc.NfcState
import com.example.solvare.ui.components.NfcBottomSheet
import com.example.solvare.ui.components.TransactionStatusDialog
import com.example.solvare.viewmodel.NfcPurpose
import com.example.solvare.viewmodel.TxStatus
import com.example.solvare.viewmodel.WalletViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    viewModel: WalletViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val nfcState by viewModel.nfcState.collectAsState()
    val nfcMode by viewModel.nfcManager.mode.collectAsState()
    val nfcErrorDetail by viewModel.nfcManager.lastErrorDetail.collectAsState()
    val activity = LocalContext.current as ComponentActivity

    DisposableEffect(activity) {
        viewModel.nfcManager.enableForegroundDispatch(activity)
        onDispose {
            viewModel.nfcManager.disableForegroundDispatch(activity)
        }
    }

    DisposableEffect(Unit) {
        viewModel.saveNavDestination("send")
        onDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Надіслати SOL") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.balanceSol != null) {
                Text(
                    text = "Доступно: ${String.format(Locale.US, "%.4f", uiState.balanceSol)} SOL",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            OutlinedTextField(
                value = uiState.sendRecipient,
                onValueChange = { viewModel.onRecipientChanged(it) },
                label = { Text("Адреса отримувача") },
                placeholder = { Text("Base58 Solana адреса") },
                isError = uiState.recipientError != null,
                supportingText = uiState.recipientError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.sendAmount,
                onValueChange = { viewModel.onAmountChanged(it) },
                label = { Text("Сума (SOL)") },
                placeholder = { Text("0.0") },
                isError = uiState.amountError != null,
                supportingText = uiState.amountError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            if (uiState.sendError != null && uiState.txStatus != TxStatus.ERROR) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.sendError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.onSendClicked() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState.txStatus == TxStatus.IDLE,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = "Підписати та надіслати",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (uiState.showNfcSheet && uiState.nfcPurpose == NfcPurpose.SIGN_TRANSACTION) {
            NfcBottomSheet(
                nfcState = nfcState,
                purpose = NfcPurpose.SIGN_TRANSACTION,
                nfcMode = nfcMode,
                onDismiss = { viewModel.onNfcSheetDismissed() },
                errorDetail = nfcErrorDetail
            )
        }

        if (uiState.txStatus != TxStatus.IDLE && uiState.txStatus != TxStatus.AWAITING_SIGN) {
            TransactionStatusDialog(
                txStatus = uiState.txStatus,
                txSignature = uiState.txSignature,
                errorMessage = uiState.sendError,
                onDismiss = {
                    viewModel.resetTxStatus()
                    if (uiState.txStatus == TxStatus.CONFIRMED) onNavigateBack()
                }
            )
        }
    }
}
