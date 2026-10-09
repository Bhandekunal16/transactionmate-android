package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionItem
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebitRed
import com.example.viewmodel.ConnectionState
import java.text.DecimalFormat
import java.util.Locale

private val currencyFormatter = DecimalFormat("#,##0.00")

fun formatCurrency(amount: Double, symbol: String = "₹"): String {
    return "$symbol ${currencyFormatter.format(amount)}"
}

@Composable
fun CurrencyText(
    amount: Double,
    currency: String = "₹",
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = formatCurrency(amount, currency),
        style = style,
        color = color,
        modifier = modifier
    )
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase(Locale.ROOT)) {
        "food", "dining", "groceries" -> Icons.Default.Restaurant
        "shopping", "ecommerce" -> Icons.Default.ShoppingBag
        "salary", "income", "freelance" -> Icons.Default.Paid
        "bills", "utilities", "electricity" -> Icons.Default.Receipt
        "health", "medical" -> Icons.Default.LocalHospital
        "travel", "transport", "fuel" -> Icons.Default.DirectionsCar
        "entertainment", "movies", "games" -> Icons.Default.Movie
        "investment", "stocks", "savings" -> Icons.Default.TrendingUp
        "transfer" -> Icons.Default.SwapHoriz
        else -> Icons.Default.AccountBalanceWallet
    }
}

@Composable
fun TransactionRow(
    transaction: TransactionItem,
    currencySymbol: String = "₹",
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .minimumInteractiveComponentSize()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("transaction_item_${transaction.id ?: transaction.amount}"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val icon = getCategoryIcon(transaction.category)
            val iconBg = if (transaction.isCredit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            val iconTint = if (transaction.isCredit) CreditGreen else DebitRed

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.category,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifBlank { transaction.category },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.bankName.ifBlank { "Bank" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = transaction.displayDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount
            val prefix = if (transaction.isCredit) "+" else "-"
            val amountColor = if (transaction.isCredit) CreditGreen else DebitRed

            Text(
                text = "$prefix$currencySymbol ${currencyFormatter.format(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@Composable
fun ServerConfigDialog(
    currentUrl: String,
    connectionState: ConnectionState,
    onDismiss: () -> Unit,
    onSaveUrl: (String) -> Unit,
    onTestPing: () -> Unit
) {
    var inputUrl by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = "Server Config",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Backend Server API")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Configure the base URL of your TransactionMate backend. Android emulator connects to host via 10.0.2.2.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text("Base URL") },
                    placeholder = { Text("http://10.0.2.2:5000") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_url_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Preset chips
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = inputUrl.contains("147.224.251.137:8001"),
                        onClick = { inputUrl = "http://147.224.251.137:8001" },
                        label = { Text("Main Server") }
                    )
                    FilterChip(
                        selected = inputUrl.contains("10.0.2.2:5000"),
                        onClick = { inputUrl = "http://10.0.2.2:5000" },
                        label = { Text("10.0.2.2:5000") }
                    )
                    FilterChip(
                        selected = inputUrl.contains("10.0.2.2:8000"),
                        onClick = { inputUrl = "http://10.0.2.2:8000" },
                        label = { Text("10.0.2.2:8000") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Connection status indicator
                when (connectionState) {
                    is ConnectionState.Checking -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing connection (GET /)...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is ConnectionState.Connected -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, "Connected", tint = CreditGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Connected: ${connectionState.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CreditGreen
                                )
                            }
                        }
                    }
                    is ConnectionState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, "Error", tint = DebitRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = connectionState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DebitRed
                                )
                            }
                        }
                    }
                    ConnectionState.Idle -> Unit
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onTestPing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Ping", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Connection (Ping GET /)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveUrl(inputUrl)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_server_url_button")
            ) {
                Text("Save & Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConnectionStatusBar(
    connectionState: ConnectionState,
    baseUrl: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon, label) = when (connectionState) {
        is ConnectionState.Connected -> Quadruple(
            Color(0xFFE8F5E9),
            CreditGreen,
            Icons.Default.CheckCircle,
            "Backend Online • ${baseUrl.removePrefix("http://").removePrefix("https://").trimEnd('/')}"
        )
        is ConnectionState.Checking -> Quadruple(
            Color(0xFFFFF3CD),
            Color(0xFF856404),
            Icons.Default.Sync,
            "Connecting to backend..."
        )
        is ConnectionState.Error -> Quadruple(
            Color(0xFFFFEBEE),
            DebitRed,
            Icons.Default.ErrorOutline,
            "Offline / Unreachable (${baseUrl.removePrefix("http://").removePrefix("https://").trimEnd('/')})"
        )
        is ConnectionState.Idle -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Dns,
            "Server: $baseUrl"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenSettings() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Status",
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Edit Server",
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun EmptyStateCard(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.Inbox,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (actionButtonText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onActionClick,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(actionButtonText)
                }
            }
        }
    }
}
