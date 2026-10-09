package com.example.ui.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.viewmodel.TransactionMateViewModel
import java.text.SimpleDateFormat
import java.util.*

val CATEGORIES = listOf(
    "All", "Food", "Shopping", "Salary", "Bills", "Health", "Travel", "Entertainment", "Transfer", "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionMateViewModel,
    initialAddCredit: Boolean? = null,
    onResetAddTransactionTrigger: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val txnsState by viewModel.transactionsState.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var addIsCredit by remember { mutableStateOf(false) }

    LaunchedEffect(initialAddCredit) {
        if (initialAddCredit != null) {
            addIsCredit = initialAddCredit
            showAddDialog = true
            onResetAddTransactionTrigger()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Transactions",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadTransactions() },
                        modifier = Modifier.testTag("refresh_transactions_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    addIsCredit = false
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Record Txn") },
                modifier = Modifier.testTag("add_transaction_fab")
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
            // Search field
            OutlinedTextField(
                value = txnsState.searchQuery,
                onValueChange = { viewModel.filterTransactions(search = it) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search by description, bank, or category...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter row: Type (All, Debit, Credit)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL" to "All", "DEBIT" to "Debits (-)", "CREDIT" to "Credits (+)").forEach { (typeKey, label) ->
                    FilterChip(
                        selected = txnsState.filterType == typeKey,
                        onClick = { viewModel.filterTransactions(type = typeKey) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("filter_type_$typeKey")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Categories horizontal scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CATEGORIES.forEach { cat ->
                    FilterChip(
                        selected = txnsState.filterCategory.equals(cat, ignoreCase = true),
                        onClick = { viewModel.filterTransactions(category = cat) },
                        label = { Text(cat) },
                        modifier = Modifier.testTag("filter_cat_$cat")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Loading / Error / List
            when {
                txnsState.isLoading && txnsState.allTransactions.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                txnsState.filteredTransactions.isEmpty() -> {
                    EmptyStateCard(
                        title = "No Transactions Found",
                        description = if (txnsState.searchQuery.isNotBlank() || txnsState.filterType != "ALL" || txnsState.filterCategory != "ALL") {
                            "No transactions match the selected filters. Try resetting the filters or record a new transaction."
                        } else {
                            "No transactions recorded yet. Tap below to record your first debit or credit."
                        },
                        icon = Icons.Default.ReceiptLong,
                        actionButtonText = "+ Record Transaction",
                        onActionClick = {
                            addIsCredit = false
                            showAddDialog = true
                        }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                    ) {
                        items(txnsState.filteredTransactions) { item ->
                            TransactionRow(
                                transaction = item,
                                currencySymbol = currency
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        RecordTransactionDialog(
            isCreditInitial = addIsCredit,
            bankAccounts = userProfile?.accounts ?: emptyList(),
            onDismiss = { showAddDialog = false },
            onSubmit = { amount, bankName, category, description, date, type, accountNum, onResult ->
                viewModel.recordPayment(
                    amount = amount,
                    bankName = bankName,
                    category = category,
                    description = description,
                    transactionDate = date,
                    type = type,
                    accountNumber = accountNum,
                    onComplete = { success, msg ->
                        onResult(success, msg)
                        if (success) {
                            showAddDialog = false
                        }
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTransactionDialog(
    isCreditInitial: Boolean,
    bankAccounts: List<com.example.data.model.BankAccount>,
    onDismiss: () -> Unit,
    onSubmit: (
        amount: Double,
        bankName: String,
        category: String,
        description: String,
        date: String,
        type: String,
        accountNumber: String?,
        onResult: (Boolean, String) -> Unit
    ) -> Unit
) {
    var isCredit by remember { mutableStateOf(isCreditInitial) }
    var amountText by remember { mutableStateOf("") }
    var bankName by remember {
        mutableStateOf(bankAccounts.firstOrNull()?.bankName ?: "Primary Bank")
    }
    var accountNumber by remember {
        mutableStateOf(bankAccounts.firstOrNull()?.accountNumber ?: "")
    }
    var category by remember { mutableStateOf("Food") }
    var description by remember { mutableStateOf("") }

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    var transactionDate by remember { mutableStateOf(todayDate) }

    var validationError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isCredit) "Record Credit (+ Income)" else "Record Debit (- Expense)",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isCredit,
                        onClick = { isCredit = false },
                        label = { Text("Debit (- Expense)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isCredit,
                        onClick = { isCredit = true },
                        label = { Text("Credit (+ Income)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        validationError = null
                    },
                    label = { Text("Amount *") },
                    placeholder = { Text("e.g. 250.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("txn_amount_input")
                )

                // Bank Name
                if (bankAccounts.isNotEmpty()) {
                    Text(
                        text = "Linked Bank Accounts (Quick Select):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bankAccounts.forEach { acc ->
                            FilterChip(
                                selected = bankName == acc.bankName,
                                onClick = {
                                    bankName = acc.bankName
                                    accountNumber = acc.accountNumber
                                },
                                label = { Text(acc.bankName) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank / Account Name *") },
                    placeholder = { Text("e.g. HDFC Bank, SBI") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("txn_bank_input")
                )

                // Category Quick Select
                Text(
                    text = "Category (Quick Select):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Food", "Shopping", "Salary", "Bills", "Health", "Travel", "Transfer").forEach { cat ->
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
                    label = { Text("Category") },
                    placeholder = { Text("e.g. Food, Salary, Bills") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Dinner with friends") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date
                OutlinedTextField(
                    value = transactionDate,
                    onValueChange = { transactionDate = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
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
                        validationError = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    if (bankName.isBlank()) {
                        validationError = "Bank Name is required"
                        return@Button
                    }
                    if (transactionDate.isBlank()) {
                        validationError = "Transaction date is required"
                        return@Button
                    }

                    isSubmitting = true
                    validationError = null
                    onSubmit(
                        amt,
                        bankName.trim(),
                        category.trim().ifBlank { "General" },
                        description.trim().ifBlank { category.trim() },
                        transactionDate.trim(),
                        if (isCredit) "credit" else "debit",
                        accountNumber.ifBlank { null }
                    ) { success, msg ->
                        isSubmitting = false
                        if (!success) {
                            validationError = msg
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.testTag("submit_transaction_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Submit to API")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
