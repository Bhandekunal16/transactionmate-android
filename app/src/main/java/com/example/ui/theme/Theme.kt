package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun TransactionMateTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    themeColor: AppThemeColor = AppThemeColor.GREEN,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemDark
    }

    val context = LocalContext.current
    val colorScheme = ThemePalettes.getColorScheme(
        context = context,
        themeColor = themeColor,
        isDark = isDark
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards compatibility overload
@Composable
fun TransactionMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val mode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT
    val color = if (dynamicColor) AppThemeColor.DYNAMIC else AppThemeColor.GREEN
    TransactionMateTheme(themeMode = mode, themeColor = color, content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    TransactionMateTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
