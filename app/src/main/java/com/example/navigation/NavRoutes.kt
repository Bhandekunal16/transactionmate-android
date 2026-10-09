package com.example.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard),
    TRANSACTIONS("transactions", "Transactions", Icons.Default.ReceiptLong),
    BUDGETS("budgets", "Budgets", Icons.Default.Savings),
    QR("qr", "Payment QR", Icons.Default.QrCode2),
    PROFILE("profile", "Profile", Icons.Default.Person),
    APPEARANCE("appearance", "Appearance", Icons.Default.Palette)
}
