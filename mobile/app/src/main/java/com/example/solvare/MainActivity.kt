package com.example.solvare

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.solvare.ui.screens.HistoryScreen
import com.example.solvare.ui.screens.HomeScreen
import com.example.solvare.ui.screens.OnboardingScreen
import com.example.solvare.ui.screens.SendScreen
import com.example.solvare.ui.screens.SettingsScreen
import com.example.solvare.ui.theme.SolvareTheme
import com.example.solvare.viewmodel.WalletViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WalletViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                return WalletViewModel(
                    savedStateHandle = extras.createSavedStateHandle(),
                    context = applicationContext
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SolvareTheme {
                SolvareNavGraph(viewModel = viewModel)
            }
        }
        dispatchNfcIntentIfNeeded(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatchNfcIntentIfNeeded(intent)
    }

    private fun dispatchNfcIntentIfNeeded(intent: Intent?) {
        if (intent == null) return
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action
        ) {
            viewModel.nfcManager.handleIntent(intent)
        }
    }
}

@Composable
fun SolvareNavGraph(viewModel: WalletViewModel) {
    val navController = rememberNavController()
    val startDestination = remember {
        if (viewModel.shouldShowOnboarding()) "onboarding" else viewModel.getInitialNavDestination()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreen(
                viewModel = viewModel,
                onFinish = {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSend = { navController.navigate("send") },
                onTab = { route -> navigateTab(navController, route) },
            )
        }
        composable("history") {
            HistoryScreen(
                viewModel = viewModel,
                onTab = { route -> navigateTab(navController, route) },
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onTab = { route -> navigateTab(navController, route) },
            )
        }
        composable("send") {
            SendScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

private fun navigateTab(
    navController: androidx.navigation.NavController,
    route: String,
) {
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
