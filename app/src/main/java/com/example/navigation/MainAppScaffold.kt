package com.example.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.budgets.BudgetsScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.qr.QrScreen
import com.example.ui.transactions.TransactionsScreen
import com.example.viewmodel.TransactionMateViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainAppScaffold(
    viewModel: TransactionMateViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

    // Quick action triggers passed from Dashboard to other screens
    var triggerAddTransactionCredit by remember { mutableStateOf<Boolean?>(null) }
    var triggerAddBudget by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Listen to action status notifications
    LaunchedEffect(viewModel) {
        viewModel.actionStatusMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Handle back button on sub-screens
    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        currentScreen = if (currentScreen == Screen.APPEARANCE) Screen.PROFILE else Screen.DASHBOARD
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (currentScreen != Screen.APPEARANCE) {
                NavigationBar(
                    modifier = Modifier
                        .testTag("bottom_nav_bar")
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Screen.entries.filter { it != Screen.APPEARANCE }.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            modifier = Modifier.testTag("nav_tab_${screen.route}")
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTransactions = { currentScreen = Screen.TRANSACTIONS },
                        onNavigateToBudgets = { currentScreen = Screen.BUDGETS },
                        onNavigateToQr = { currentScreen = Screen.QR },
                        onOpenAddTransaction = { isCredit ->
                            triggerAddTransactionCredit = isCredit
                            currentScreen = Screen.TRANSACTIONS
                        },
                        onOpenAddBudget = {
                            triggerAddBudget = true
                            currentScreen = Screen.BUDGETS
                        }
                    )
                }
                Screen.TRANSACTIONS -> {
                    TransactionsScreen(
                        viewModel = viewModel,
                        initialAddCredit = triggerAddTransactionCredit,
                        onResetAddTransactionTrigger = { triggerAddTransactionCredit = null }
                    )
                }
                Screen.BUDGETS -> {
                    BudgetsScreen(
                        viewModel = viewModel,
                        initialAddBudget = triggerAddBudget,
                        onResetAddBudgetTrigger = { triggerAddBudget = false }
                    )
                }
                Screen.QR -> {
                    QrScreen(viewModel = viewModel)
                }
                Screen.PROFILE -> {
                    ProfileScreen(
                        viewModel = viewModel,
                        onNavigateToAppearance = { currentScreen = Screen.APPEARANCE }
                    )
                }
                Screen.APPEARANCE -> {
                    com.example.ui.settings.AppearanceScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.PROFILE }
                    )
                }
            }
        }
    }
}
