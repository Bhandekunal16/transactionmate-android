package com.example.ui.qr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BankAccount
import com.example.data.model.QrItem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.QrDisplayView
import com.example.ui.components.formatCurrency
import com.example.viewmodel.TransactionMateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScreen(
    viewModel: TransactionMateViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val qrState by viewModel.qrState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    var amountInput by remember { mutableStateOf("") }
    val accounts = userProfile?.accounts.orEmpty()
    var selectedAccount by remember(accounts) { mutableStateOf<BankAccount?>(accounts.firstOrNull()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment QR Codes", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.loadQrCodes(
                                bankName = selectedAccount?.bankName,
                                upiVpa = selectedAccount?.upiVpa,
                                amount = amountInput.toDoubleOrNull()
                            )
                        },
                        modifier = Modifier.testTag("refresh_qr_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh QR")
                    }
                }
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
            // Optional Amount & Account Selector Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Customize QR Code",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (accounts.isNotEmpty()) {
                        Text(
                            text = "Select Account:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            accounts.forEach { acc ->
                                FilterChip(
                                    selected = selectedAccount?.accountNumber == acc.accountNumber,
                                    onClick = {
                                        selectedAccount = acc
                                        viewModel.loadQrCodes(
                                            bankName = acc.bankName,
                                            upiVpa = acc.upiVpa,
                                            amount = amountInput.toDoubleOrNull()
                                        )
                                    },
                                    label = { Text(acc.bankName.ifBlank { "Account" }) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Amount (Optional)") },
                            placeholder = { Text("e.g. 500") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("qr_amount_input")
                        )

                        Button(
                            onClick = {
                                viewModel.loadQrCodes(
                                    bankName = selectedAccount?.bankName,
                                    upiVpa = selectedAccount?.upiVpa,
                                    amount = amountInput.toDoubleOrNull()
                                )
                            },
                            modifier = Modifier.testTag("generate_qr_button")
                        ) {
                            Text("Generate")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                qrState.isLoading && qrState.qrList.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                qrState.qrList.isEmpty() -> {
                    // If backend list is empty, generate from selected bank account or show empty state
                    if (selectedAccount != null && selectedAccount!!.upiVpa.isNotBlank()) {
                        val fallbackQr = QrItem(
                            bankName = selectedAccount!!.bankName,
                            accountNumber = selectedAccount!!.accountNumber,
                            upiVpa = selectedAccount!!.upiVpa,
                            qrCode = buildUpiUri(selectedAccount!!.upiVpa, selectedAccount!!.bankName, amountInput.toDoubleOrNull()),
                            amount = amountInput.toDoubleOrNull()
                        )
                        QrCardItem(
                            qrItem = fallbackQr,
                            currency = currency,
                            onShare = { shareQr(context, fallbackQr) },
                            onCopyUpi = { copyUpi(context, fallbackQr.upiVpa.orEmpty()) }
                        )
                    } else {
                        EmptyStateCard(
                            title = "No QR Codes Available",
                            description = "Link a bank account with UPI VPA or tap Generate to request a payment QR code from the server.",
                            icon = Icons.Default.QrCode2,
                            actionButtonText = "Request from API",
                            onActionClick = {
                                viewModel.loadQrCodes(
                                    bankName = selectedAccount?.bankName,
                                    upiVpa = selectedAccount?.upiVpa,
                                    amount = amountInput.toDoubleOrNull()
                                )
                            }
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(qrState.qrList) { qrItem ->
                            QrCardItem(
                                qrItem = qrItem,
                                currency = currency,
                                onShare = { shareQr(context, qrItem) },
                                onCopyUpi = { copyUpi(context, qrItem.upiVpa.orEmpty()) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QrCardItem(
    qrItem: QrItem,
    currency: String,
    onShare: () -> Unit,
    onCopyUpi: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("qr_card_item")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = qrItem.bankName ?: "UPI Payment Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (qrItem.upiVpa != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = qrItem.upiVpa,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QR Display Area
            QrDisplayView(
                qrContent = qrItem.effectiveQrContent,
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (qrItem.amount != null && qrItem.amount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Requested Amount: ${formatCurrency(qrItem.amount, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyUpi,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy UPI")
                }

                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f).testTag("share_qr_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
    }
}

private fun buildUpiUri(vpa: String, name: String, amount: Double?): String {
    val cleanName = name.replace(" ", "%20")
    var uri = "upi://pay?pa=$vpa&pn=$cleanName&cu=INR"
    if (amount != null && amount > 0) {
        uri += "&am=$amount"
    }
    return uri
}

private fun shareQr(context: Context, qrItem: QrItem) {
    val shareText = buildString {
        append("Pay via UPI using TransactionMate\n")
        if (!qrItem.bankName.isNullOrBlank()) append("Bank: ${qrItem.bankName}\n")
        if (!qrItem.upiVpa.isNullOrBlank()) append("UPI VPA: ${qrItem.upiVpa}\n")
        if (qrItem.amount != null && qrItem.amount > 0) append("Amount: ₹ ${qrItem.amount}\n")
        if (qrItem.effectiveQrContent.isNotBlank()) append("QR / Payment Link: ${qrItem.effectiveQrContent}")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Payment QR Details")
    context.startActivity(shareIntent)
}

private fun copyUpi(context: Context, upiVpa: String) {
    if (upiVpa.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("UPI VPA", upiVpa))
    Toast.makeText(context, "UPI VPA copied to clipboard", Toast.LENGTH_SHORT).show()
}
