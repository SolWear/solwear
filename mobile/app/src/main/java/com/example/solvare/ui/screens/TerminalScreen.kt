package com.example.solvare.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.solvare.viewmodel.WalletViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.solvare.R
import com.example.solvare.terminal.crypto.TerminalKeyManager
import com.example.solvare.terminal.nfc.SolvareHostApduService
import com.example.solvare.ui.theme.TerminalAmber
import com.example.solvare.ui.theme.TerminalGreen
import com.example.solvare.ui.theme.TerminalScreenTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: WalletViewModel,
    onNavigateBack: () -> Unit
) {
    TerminalScreenTheme {
        val context = LocalContext.current
        val activity = context as ComponentActivity
        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(activity) {
            viewModel.nfcManager.disableForegroundDispatch(activity)
            onDispose { }
        }

        DisposableEffect(Unit) {
            viewModel.saveNavDestination("terminal")
            onDispose { }
        }

        val keyManager = remember {
            TerminalKeyManager(activity.applicationContext)
        }

        var nfcEnabled by remember { mutableStateOf(false) }
        var lastEvent by remember { mutableStateOf("Очікування підключення...") }
        var eventLog by remember { mutableStateOf(listOf<String>()) }

        val hceComponent = remember {
            ComponentName(activity, SolvareHostApduService::class.java)
        }

        DisposableEffect(lifecycleOwner, hceComponent) {
            val observer = LifecycleEventObserver { _, event ->
                val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return@LifecycleEventObserver
                val emulation = CardEmulation.getInstance(adapter)
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        if (adapter.isEnabled &&
                            !emulation.isDefaultServiceForCategory(hceComponent, CardEmulation.CATEGORY_OTHER)
                        ) {
                            emulation.setPreferredService(activity, hceComponent)
                        }
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        emulation.unsetPreferredService(activity)
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                val adapter = NfcAdapter.getDefaultAdapter(activity)
                if (adapter != null && adapter.isEnabled) {
                    val emulation = CardEmulation.getInstance(adapter)
                    if (!emulation.isDefaultServiceForCategory(hceComponent, CardEmulation.CATEGORY_OTHER)) {
                        emulation.setPreferredService(activity, hceComponent)
                    }
                }
            }
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                runCatching {
                    val adapter = NfcAdapter.getDefaultAdapter(activity)
                    if (adapter != null) {
                        CardEmulation.getInstance(adapter).unsetPreferredService(activity)
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            val adapter = NfcAdapter.getDefaultAdapter(context)
            nfcEnabled = adapter?.isEnabled == true

            SolvareHostApduService.eventCallback = { event ->
                lastEvent = event
                eventLog =
                    (listOf("[${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}] $event") + eventLog).take(50)
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                SolvareHostApduService.eventCallback = null
            }
        }

        val statusColor by animateColorAsState(
            if (nfcEnabled) TerminalGreen else TerminalAmber,
            label = "status_color"
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Watch,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Solvare Terminal", style = MaterialTheme.typography.titleMedium)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (nfcEnabled) "NFC HCE активний" else "NFC вимкнений",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Емуляція NDEF Type 4 Tag",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ПУБЛІЧНИЙ КЛЮЧ (DEVNET)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = keyManager.publicKeyBase58,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            OutlinedButton(
                                onClick = {
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText("pubkey", keyManager.publicKeyBase58)
                                    )
                                    Toast.makeText(context, "Адресу скопійовано", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Копіювати", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Nfc,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Телефон емулює NFC-годинник",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Піднесіть основний телефон із\nзастосунком Solvare до цього пристрою",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.hce_payment_conflict_hint),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            SolvareHostApduService.resetLiveServiceToWalletMode()
                            lastEvent = "Скинуто в режим гаманця"
                            eventLog = (listOf("[reset] Скидання в режим wallet") + eventLog).take(50)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Скинути", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 300.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ЖУРНАЛ ПОДІЙ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (eventLog.isEmpty()) {
                            Text(
                                text = lastEvent,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column {
                                eventLog.forEach { event ->
                                    Text(
                                        text = event,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Для поповнення devnet-балансу використовуйте:\nsol airdrop 2 ${keyManager.publicKeyBase58.take(12)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}
