package com.example.solvare.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.solvare.nfc.NfcState
import com.example.solvare.ui.components.NfcBottomSheet
import com.example.solvare.viewmodel.NfcPurpose
import com.example.solvare.viewmodel.WalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: WalletViewModel,
    onFinish: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val nfcState by viewModel.nfcState.collectAsState()
    val nfcErrorDetail by viewModel.nfcManager.lastErrorDetail.collectAsState()
    val activity = LocalContext.current as ComponentActivity
    var page by rememberSaveable { mutableStateOf(0) }

    DisposableEffect(activity) {
        viewModel.nfcManager.enableForegroundDispatch(activity)
        onDispose {
            viewModel.nfcManager.disableForegroundDispatch(activity)
        }
    }

    LaunchedEffect(uiState.pubkey) {
        if (uiState.pubkey != null) {
            viewModel.completeOnboarding()
            viewModel.saveNavDestination("home")
            onFinish()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Вітання в Solvare") },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.completeOnboarding()
                            viewModel.saveNavDestination("home")
                            onFinish()
                        }
                    ) {
                        Text("Пропустити")
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            OnboardingPageIndicator(currentPage = page, pageCount = 3)
            Spacer(modifier = Modifier.height(28.dp))

            when (page) {
                0 -> OnboardingIntroPage()
                1 -> OnboardingStepsPage()
                else -> OnboardingConnectPage(
                    isConnected = uiState.pubkey != null,
                    onConnectClick = { viewModel.onRefreshClicked() }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (page > 0) {
                    OutlinedButton(
                        onClick = { page-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Назад")
                    }
                }

                Button(
                    onClick = {
                        if (page < 2) {
                            page++
                        } else {
                            viewModel.completeOnboarding()
                            viewModel.saveNavDestination("home")
                            onFinish()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (page < 2) "Далі" else "Готово")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
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

@Composable
private fun OnboardingIntroPage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.Watch,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Це застосунок для годинників Solvare",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Тут ви можете підключити Solvare Watch, переглядати баланс SOL і підписувати транзакції через NFC.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardingStepsPage() {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = "Як підключити годинник:",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "1. Увімкніть Solvare Watch і відкрийте екран підключення NFC.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "2. Увімкніть NFC на телефоні та годиннику.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "3. Піднесіть годинник до телефона та дочекайтесь зчитування.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun OnboardingConnectPage(
    isConnected: Boolean,
    onConnectClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = if (isConnected) Icons.Filled.CheckCircle else Icons.Filled.Nfc,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isConnected) "Годинник підключено" else "Спробуйте підключити Solvare Watch зараз",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (isConnected) {
                "Підключення успішне. Можна переходити до головного меню."
            } else {
                "Натисніть кнопку нижче та піднесіть годинник до телефона."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(onClick = onConnectClick) {
            Text(if (isConnected) "Підключити повторно" else "Підключити годинник")
        }
    }
}

@Composable
private fun OnboardingPageIndicator(
    currentPage: Int,
    pageCount: Int
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 10.dp else 8.dp)
                    .background(
                        color = if (index == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}
