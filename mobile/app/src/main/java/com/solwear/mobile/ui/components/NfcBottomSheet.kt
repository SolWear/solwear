package com.solwear.mobile.ui.components

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
import com.solwear.mobile.nfc.NfcMode
import com.solwear.mobile.nfc.NfcState
import com.solwear.mobile.viewmodel.NfcPurpose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcBottomSheet(
    nfcState: NfcState,
    purpose: NfcPurpose,
    onDismiss: () -> Unit,
    nfcMode: NfcMode? = null,
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
                                "Hold SolWear near this phone"
                            NfcPurpose.SIGN_TRANSACTION -> when (nfcMode) {
                                NfcMode.READ_SIGN_RESPONSE ->
                                    "Tap again to read the signature"
                                else ->
                                    "Hold SolWear near this phone to sign"
                            }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (purpose) {
                            NfcPurpose.READ_WALLET ->
                                "Primary mode reads the watch. Fallback mode lets the watch read this phone for a guaranteed tap effect."
                            NfcPurpose.SIGN_TRANSACTION ->
                                "Signing needs two taps: write the request, confirm on SolWear, then read the response."
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
                        text = if (nfcState == NfcState.READING) "Reading data..." else "Writing data...",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                NfcState.SUCCESS -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Success",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Synced",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                NfcState.ERROR -> {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "Error",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "NFC error",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorDetail?.takeIf { it.isNotBlank() }
                            ?: "Try again",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
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
