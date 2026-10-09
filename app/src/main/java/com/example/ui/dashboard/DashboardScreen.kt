package com.example.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DateWiseStat
import com.example.ui.components.*
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebitRed
import com.example.ui.theme.ForestGreenPrimary
import com.example.viewmodel.ConnectionState
import com.example.viewmodel.TransactionMateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TransactionMateViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToQr: () -> Unit,
    onOpenAddTransaction: (isCredit: Boolean) -> Unit,
    onOpenAddBudget: () -> Unit,
    onOpenServerConfig: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()
    val activeUserName by viewModel.activeUserName.collectAsState()
    val activeUsername by viewModel.activeUsername.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hello, $activeUserName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@$activeUsername",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshAll() },
                        modifier = Modifier.testTag("refresh_dashboard_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection banner - only show if error with user-friendly retry message
            if (connectionState is ConnectionState.Error) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.refreshAll() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Unable to connect. Tap to retry.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Hero Balance Card
            item {
                HeroBalanceCard(
                    totalBalance = dashboardState.totalBalance,
                    totalIncome = dashboardState.totalIncome,
                    totalExpenses = dashboardState.totalExpenses,
                    currencySymbol = currency
                )
            }

            // Quick Actions
            item {
                QuickActionsRow(
                    onAddIncome = { onOpenAddTransaction(true) },
                    onAddExpense = { onOpenAddTransaction(false) },
                    onAddBudget = onOpenAddBudget,
                    onShowQr = onNavigateToQr
                )
            }

            // Monthly Spending Card
            item {
                MonthlySpendingCard(
                    monthlyDebit = dashboardState.monthlyDebitTotal,
                    currencySymbol = currency,
                    onViewBudgets = onNavigateToBudgets
                )
            }

            // Statistics Chart
            item {
                SpendingStatisticsChartCard(
                    statistics = dashboardState.statistics,
                    currencySymbol = currency
                )
            }

            // Recent Transactions Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onNavigateToTransactions,
                        modifier = Modifier.testTag("view_all_transactions_button")
                    ) {
                        Text("View All")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Recent Transactions List
            if (dashboardState.recentTransactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Transactions Recorded",
                        description = "Add your first income or expense to see transaction history and reports.",
                        icon = Icons.Default.ReceiptLong,
                        actionButtonText = "+ Add Transaction",
                        onActionClick = { onOpenAddTransaction(false) }
                    )
                }
            } else {
                items(dashboardState.recentTransactions) { txn ->
                    TransactionRow(
                        transaction = txn,
                        currencySymbol = currency,
                        onClick = onNavigateToTransactions
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HeroBalanceCard(
    totalBalance: Double,
    totalIncome: Double,
    totalExpenses: Double,
    currencySymbol: String
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_balance_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryColor,
                            primaryColor.copy(alpha = 0.82f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onPrimaryColor.copy(alpha = 0.85f)
                    )
                    Surface(
                        color = onPrimaryColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Live Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = onPrimaryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formatCurrency(totalBalance, currencySymbol),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = onPrimaryColor
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Income chip
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = onPrimaryColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = "Income",
                                    tint = CreditGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Income",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onPrimaryColor.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = formatCurrency(totalIncome, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onPrimaryColor
                                )
                            }
                        }
                    }

                    // Expense chip
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = onPrimaryColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFEBEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = "Expense",
                                    tint = DebitRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Expenses",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onPrimaryColor.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = formatCurrency(totalExpenses, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onPrimaryColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddBudget: () -> Unit,
    onShowQr: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            QuickActionButton(
                icon = Icons.Default.AddCircle,
                label = "+ Income",
                color = CreditGreen,
                onClick = onAddIncome
            )
            QuickActionButton(
                icon = Icons.Default.RemoveCircle,
                label = "- Expense",
                color = DebitRed,
                onClick = onAddExpense
            )
            QuickActionButton(
                icon = Icons.Default.Savings,
                label = "Add Budget",
                color = MaterialTheme.colorScheme.primary,
                onClick = onAddBudget
            )
            QuickActionButton(
                icon = Icons.Default.QrCode2,
                label = "My QR",
                color = MaterialTheme.colorScheme.secondary,
                onClick = onShowQr
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .minimumInteractiveComponentSize()
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MonthlySpendingCard(
    monthlyDebit: Double,
    currencySymbol: String,
    onViewBudgets: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Current Month's Debit Total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrency(monthlyDebit, currencySymbol),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            FilledTonalButton(
                onClick = onViewBudgets,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Budgets")
            }
        }
    }
}

@Composable
private fun SpendingStatisticsChartCard(
    statistics: com.example.data.model.TxnStatisticsResponse?,
    currencySymbol: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Debits vs Credits",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val debitTotal = statistics?.debitTotal ?: 0.0
            val creditTotal = statistics?.creditTotal ?: 0.0
            val debitCount = statistics?.debitCount ?: 0
            val creditCount = statistics?.creditCount ?: 0

            // Ratio Bar Chart
            val total = debitTotal + creditTotal
            val debitFraction = if (total > 0) (debitTotal / total).toFloat() else 0.5f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE0E0E0))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(debitFraction.coerceIn(0.05f, 0.95f))
                            .background(DebitRed)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight((1f - debitFraction).coerceIn(0.05f, 0.95f))
                            .background(CreditGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(DebitRed))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Debit ($debitCount txns): ${formatCurrency(debitTotal, currencySymbol)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CreditGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Credit ($creditCount txns): ${formatCurrency(creditTotal, currencySymbol)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Daily trend mini-canvas if date_wise available
            val daily = statistics?.dailyList.orEmpty()
            if (daily.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Daily Trend",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                DailyTrendCanvas(daily = daily)
            }
        }
    }
}

@Composable
private fun DailyTrendCanvas(daily: List<DateWiseStat>) {
    val items = daily.takeLast(7)
    val maxVal = items.maxOfOrNull { maxOf(it.debit, it.credit) }?.coerceAtLeast(100.0) ?: 100.0

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val barWidth = size.width / (items.size * 2.5f)
        val spacing = size.width / items.size

        items.forEachIndexed { index, stat ->
            val x = index * spacing + spacing / 4
            val debitH = (stat.debit / maxVal * size.height).toFloat().coerceAtLeast(4f)
            val creditH = (stat.credit / maxVal * size.height).toFloat().coerceAtLeast(4f)

            // Debit bar
            drawRect(
                color = DebitRed.copy(alpha = 0.85f),
                topLeft = Offset(x, size.height - debitH),
                size = Size(barWidth, debitH)
            )

            // Credit bar
            drawRect(
                color = CreditGreen.copy(alpha = 0.85f),
                topLeft = Offset(x + barWidth + 2f, size.height - creditH),
                size = Size(barWidth, creditH)
            )
        }
    }
}
