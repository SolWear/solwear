package com.example.solvare.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.solvare.viewmodel.TxStatus

@Composable
fun TransactionStatusDialog(
    txStatus: TxStatus,
    txSignature: String?,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (txStatus == TxStatus.CONFIRMED || txStatus == TxStatus.ERROR) onDismiss()
        },
        confirmButton = {
            if (txStatus == TxStatus.CONFIRMED || txStatus == TxStatus.ERROR) {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        icon = {
            when (txStatus) {
                TxStatus.BUILDING, TxStatus.SENDING, TxStatus.CONFIRMING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp
                    )
                }
                TxStatus.AWAITING_SIGN -> {
                    Icon(
                        imageVector = Icons.Filled.HourglassTop,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                TxStatus.CONFIRMED -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                TxStatus.ERROR -> {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }
        },
        title = {
            Text(
                text = when (txStatus) {
                    TxStatus.BUILDING -> "Побудова транзакції..."
                    TxStatus.AWAITING_SIGN -> "Очікування підпису..."
                    TxStatus.SENDING -> "Надсилання..."
                    TxStatus.CONFIRMING -> "Підтвердження..."
                    TxStatus.CONFIRMED -> "Транзакцію підтверджено"
                    TxStatus.ERROR -> "Помилка"
                    else -> ""
                },
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (txStatus) {
                    TxStatus.CONFIRMED -> {
                        if (txSignature != null) {
                            Text(
                                text = txSignature,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    TxStatus.ERROR -> {
                        Text(
                            text = errorMessage ?: "Невідома помилка",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {}
                }
            }
        }
    )
}
