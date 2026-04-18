package com.example.solvare.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.example.solvare.nfc.NfcMode
import com.example.solvare.nfc.NfcState
import com.example.solvare.viewmodel.NfcPurpose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcBottomSheet(
    nfcState: NfcState,
    purpose: NfcPurpose,
    onDismiss: () -> Unit,
    /** Для підпису: етап запису запиту або читання відповіді (другий дотик). */
    nfcMode: NfcMode? = null,
    /** Текст з [com.example.solvare.nfc.NfcSessionManager]; дублюється в logcat SolvareNfc. */
    errorDetail: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (nfcState) {
                NfcState.IDLE, NfcState.WAITING_FOR_TAG -> {
                    PulsingNfcIcon()
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = when (purpose) {
                            NfcPurpose.READ_WALLET ->
                                "Піднесіть годинник Solvare до цього пристрою"
                            NfcPurpose.SIGN_TRANSACTION -> when (nfcMode) {
                                NfcMode.READ_SIGN_RESPONSE ->
                                    "Піднесіть годинник вдруге — читаємо підпис"
                                else ->
                                    "Піднесіть годинник до цього пристрою для підпису"
                            }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (purpose) {
                            NfcPurpose.READ_WALLET ->
                                "Переконайтеся, що NFC на годиннику увімкнено і він поруч"
                            NfcPurpose.SIGN_TRANSACTION ->
                                "Потрібні два дотики: спочатку запит підпису, потім — читання відповіді."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                NfcState.READING, NfcState.WRITING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (nfcState == NfcState.READING) "Читання даних..." else "Запис даних...",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                NfcState.SUCCESS -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Успіх",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Готово!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                NfcState.ERROR -> {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "Помилка",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Помилка NFC",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorDetail?.takeIf { it.isNotBlank() }
                            ?: "Спробуйте ще раз",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onDismiss) {
                        Text("Закрити")
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingNfcIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nfc_alpha"
    )

    Icon(
        imageVector = Icons.Filled.Nfc,
        contentDescription = "NFC",
        modifier = Modifier
            .size(64.dp)
            .alpha(alpha),
        tint = MaterialTheme.colorScheme.primary
    )
}
