package com.example.ui.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetItem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatCurrency
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebitRed
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.TransactionMateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: TransactionMateViewModel,
    initialAddBudget: Boolean = false,
    onResetAddBudgetTrigger: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val budgetsState by viewModel.budgetsState.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var budgetToUpdate by remember { mutableStateOf<BudgetItem?>(null) }

    LaunchedEffect(initialAddBudget) {
        if (initialAddBudget) {
            showAddDialog = true
            onResetAddBudgetTrigger()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Management", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadBudgets() },
                        modifier = Modifier.testTag("refresh_budgets_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Set Budget") },
                modifier = Modifier.testTag("add_budget_fab")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Monthly Expenses Overview Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly Spending vs Total Budgets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Monthly Expenses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = formatCurrency(budgetsState.monthlySpent, currency),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        val totalBudgetLimit = budgetsState.budgets.sumOf { it.amount }
                        if (totalBudgetLimit > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Limit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = formatCurrency(totalBudgetLimit, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                budgetsState.isLoading && budgetsState.budgets.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                budgetsState.budgets.isEmpty() -> {
                    EmptyStateCard(
                        title = "No Budgets Configured",
                        description = "Create your first spending budget to monitor spending limits and stay financially disciplined.",
                        icon = Icons.Default.Savings,
                        actionButtonText = "+ Create Budget",
                        onActionClick = { showAddDialog = true }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(budgetsState.budgets) { budget ->
                            BudgetCard(
                                budget = budget,
                                monthlySpent = budgetsState.monthlySpent,
                                currency = currency,
                                onUpdateClick = { budgetToUpdate = budget }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Budget Dialog
    if (showAddDialog) {
        CreateBudgetDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { amount, category, month, onResult ->
                viewModel.addBudget(amount, category, month) { success, msg ->
                    onResult(success, msg)
                    if (success) showAddDialog = false
                }
            }
        )
    }

    // Update Budget Dialog
    budgetToUpdate?.let { targetBudget ->
        UpdateBudgetDialog(
            budget = targetBudget,
            onDismiss = { budgetToUpdate = null },
            onSubmit = { newAmount, onResult ->
                viewModel.updateBudget(
                    amount = newAmount,
                    budgetId = targetBudget.budgetId ?: targetBudget.id,
                    month = targetBudget.month
                ) { success, msg ->
                    onResult(success, msg)
                    if (success) budgetToUpdate = null
                }
            }
        )
    }
}

@Composable
fun BudgetCard(
    budget: BudgetItem,
    monthlySpent: Double,
    currency: String,
    onUpdateClick: () -> Unit
) {
    val spent = budget.spent ?: monthlySpent
    val utilization = if (budget.amount > 0) (spent / budget.amount).coerceIn(0.0, 1.5) else 0.0
    val progressFloat = utilization.toFloat().coerceIn(0f, 1f)

    val progressColor = when {
        utilization >= 1.0 -> DebitRed
        utilization >= 0.75 -> WarningAmber
        else -> CreditGreen
    }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = budget.category ?: "General Monthly Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onUpdateClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Budget",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progressFloat },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(spent, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = progressColor
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Utilization",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(utilization * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Budget Limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(budget.amount, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (utilization >= 1.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ Budget exceeded by ${formatCurrency(spent - budget.amount, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DebitRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateBudgetDialog(
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, category: String?, month: String?, onResult: (Boolean, String) -> Unit) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All Categories") }
    var month by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Budget", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMsg = null
                    },
                    label = { Text("Budget Amount *") },
                    placeholder = { Text("e.g. 15000.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("budget_amount_input")
                )

                Text(
                    text = "Quick Category:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "Food", "Shopping", "Bills").forEach { cat ->
                        FilterChip(
                            selected = category.equals(cat, ignoreCase = true),
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Optional)") },
                    placeholder = { Text("e.g. Food, Shopping, All") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = month,
                    onValueChange = { month = it },
                    label = { Text("Month (Optional, e.g. 2026-10)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMsg = "Please enter an amount greater than 0"
                        return@Button
                    }
                    isSubmitting = true
                    errorMsg = null
                    onSubmit(amt, category.ifBlank { null }, month.ifBlank { null }) { success, msg ->
                        isSubmitting = false
                        if (!success) errorMsg = msg
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.testTag("submit_budget_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save Budget")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun UpdateBudgetDialog(
    budget: BudgetItem,
    onDismiss: () -> Unit,
    onSubmit: (newAmount: Double, onResult: (Boolean, String) -> Unit) -> Unit
) {
    var amountText by remember { mutableStateOf(budget.amount.toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Budget Amount", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Category: ${budget.category ?: "General"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMsg = null
                    },
                    label = { Text("New Budget Amount *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMsg = "Please enter an amount greater than 0"
                        return@Button
                    }
                    isSubmitting = true
                    errorMsg = null
                    onSubmit(amt) { success, msg ->
                        isSubmitting = false
                        if (!success) errorMsg = msg
                    }
                },
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Update")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
