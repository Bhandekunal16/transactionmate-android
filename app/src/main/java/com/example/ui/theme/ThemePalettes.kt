package com.example.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default")
}

enum class AppThemeColor(val displayName: String, val previewColor: Color) {
    GREEN("Green", Color(0xFF0F5132)),
    BLUE("Blue", Color(0xFF0D6EFD)),
    PURPLE("Purple", Color(0xFF6F42C1)),
    TEAL("Teal", Color(0xFF008080)),
    ORANGE("Orange", Color(0xFFE65100)),
    RED("Red", Color(0xFFC62828)),
    PINK("Pink", Color(0xFFD81B60)),
    DYNAMIC("Dynamic Color", Color(0xFF6750A4))
}

object ThemePalettes {

    fun getColorScheme(
        context: Context,
        themeColor: AppThemeColor,
        isDark: Boolean
    ): ColorScheme {
        if (themeColor == AppThemeColor.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        return when (themeColor) {
            AppThemeColor.GREEN, AppThemeColor.DYNAMIC -> if (isDark) GreenDarkScheme else GreenLightScheme
            AppThemeColor.BLUE -> if (isDark) BlueDarkScheme else BlueLightScheme
            AppThemeColor.PURPLE -> if (isDark) PurpleDarkScheme else PurpleLightScheme
            AppThemeColor.TEAL -> if (isDark) TealDarkScheme else TealLightScheme
            AppThemeColor.ORANGE -> if (isDark) OrangeDarkScheme else OrangeLightScheme
            AppThemeColor.RED -> if (isDark) RedDarkScheme else RedLightScheme
            AppThemeColor.PINK -> if (isDark) PinkDarkScheme else PinkLightScheme
        }
    }

    // GREEN (Default Brand)
    val GreenLightScheme = lightColorScheme(
        primary = Color(0xFF0F5132),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD1E7DD),
        onPrimaryContainer = Color(0xFF032817),
        secondary = Color(0xFF198754),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE8F5E9),
        onSecondaryContainer = Color(0xFF0D3E24),
        background = Color(0xFFF8FBF8),
        onBackground = Color(0xFF191C1A),
        surface = Color.White,
        onSurface = Color(0xFF191C1A),
        surfaceVariant = Color(0xFFEBEFEA),
        onSurfaceVariant = Color(0xFF414942),
        outline = Color(0xFF717971)
    )

    val GreenDarkScheme = darkColorScheme(
        primary = Color(0xFF66DA98),
        onPrimary = Color(0xFF00381D),
        primaryContainer = Color(0xFF0C462B),
        onPrimaryContainer = Color(0xFF8CF8B6),
        secondary = Color(0xFF81C784),
        onSecondary = Color(0xFF003816),
        secondaryContainer = Color(0xFF1A4624),
        onSecondaryContainer = Color(0xFFA7F5AC),
        background = Color(0xFF111412),
        onBackground = Color(0xFFE1E4DF),
        surface = Color(0xFF191D1A),
        onSurface = Color(0xFFE1E4DF),
        surfaceVariant = Color(0xFF414942),
        onSurfaceVariant = Color(0xFFC1C9C0),
        outline = Color(0xFF8B938A)
    )

    // BLUE
    val BlueLightScheme = lightColorScheme(
        primary = Color(0xFF0D6EFD),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD0E1FD),
        onPrimaryContainer = Color(0xFF001B3E),
        secondary = Color(0xFF0B5ED7),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE3EDFE),
        onSecondaryContainer = Color(0xFF052C65),
        background = Color(0xFFF8F9FA),
        onBackground = Color(0xFF191C1E),
        surface = Color.White,
        onSurface = Color(0xFF191C1E),
        surfaceVariant = Color(0xFFE2E7ED),
        onSurfaceVariant = Color(0xFF42474E),
        outline = Color(0xFF72777F)
    )

    val BlueDarkScheme = darkColorScheme(
        primary = Color(0xFF82B1FF),
        onPrimary = Color(0xFF002F6C),
        primaryContainer = Color(0xFF0A387E),
        onPrimaryContainer = Color(0xFFCCE0FF),
        secondary = Color(0xFF64B5F6),
        onSecondary = Color(0xFF003258),
        secondaryContainer = Color(0xFF0D47A1),
        onSecondaryContainer = Color(0xFFBBDEFB),
        background = Color(0xFF111315),
        onBackground = Color(0xFFE2E2E6),
        surface = Color(0xFF191C1E),
        onSurface = Color(0xFFE2E2E6),
        surfaceVariant = Color(0xFF42474E),
        onSurfaceVariant = Color(0xFFC2C7CF),
        outline = Color(0xFF8C9199)
    )

    // PURPLE
    val PurpleLightScheme = lightColorScheme(
        primary = Color(0xFF6F42C1),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8DCFF),
        onPrimaryContainer = Color(0xFF22005D),
        secondary = Color(0xFF59359A),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF1E9FF),
        onSecondaryContainer = Color(0xFF2F0F66),
        background = Color(0xFFFAF8FD),
        onBackground = Color(0xFF1D1B20),
        surface = Color.White,
        onSurface = Color(0xFF1D1B20),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        outline = Color(0xFF79747E)
    )

    val PurpleDarkScheme = darkColorScheme(
        primary = Color(0xFFB388FF),
        onPrimary = Color(0xFF380094),
        primaryContainer = Color(0xFF4F1A99),
        onPrimaryContainer = Color(0xFFEBDDFF),
        secondary = Color(0xFFCE93D8),
        onSecondary = Color(0xFF3E0054),
        secondaryContainer = Color(0xFF5B1676),
        onSecondaryContainer = Color(0xFFF3DAF9),
        background = Color(0xFF141218),
        onBackground = Color(0xFFE6E1E5),
        surface = Color(0xFF1D1A22),
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),
        outline = Color(0xFF938F99)
    )

    // TEAL
    val TealLightScheme = lightColorScheme(
        primary = Color(0xFF008080),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB2DFDB),
        onPrimaryContainer = Color(0xFF002020),
        secondary = Color(0xFF00695C),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0F2F1),
        onSecondaryContainer = Color(0xFF00332C),
        background = Color(0xFFF7FAFA),
        onBackground = Color(0xFF191C1C),
        surface = Color.White,
        onSurface = Color(0xFF191C1C),
        surfaceVariant = Color(0xFFDAE5E4),
        onSurfaceVariant = Color(0xFF3F4948),
        outline = Color(0xFF6F7978)
    )

    val TealDarkScheme = darkColorScheme(
        primary = Color(0xFF4DB6AC),
        onPrimary = Color(0xFF003737),
        primaryContainer = Color(0xFF004D40),
        onPrimaryContainer = Color(0xFFB2DFDB),
        secondary = Color(0xFF80CBC4),
        onSecondary = Color(0xFF003732),
        secondaryContainer = Color(0xFF004D46),
        onSecondaryContainer = Color(0xFFA7F0E8),
        background = Color(0xFF101414),
        onBackground = Color(0xFFE0E3E2),
        surface = Color(0xFF191D1D),
        onSurface = Color(0xFFE0E3E2),
        surfaceVariant = Color(0xFF3F4948),
        onSurfaceVariant = Color(0xFFBEC9C8),
        outline = Color(0xFF889392)
    )

    // ORANGE
    val OrangeLightScheme = lightColorScheme(
        primary = Color(0xFFE65100),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFCC80),
        onPrimaryContainer = Color(0xFF331200),
        secondary = Color(0xFFBF360C),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFE0B2),
        onSecondaryContainer = Color(0xFF4A1400),
        background = Color(0xFFFFFBF8),
        onBackground = Color(0xFF201A17),
        surface = Color.White,
        onSurface = Color(0xFF201A17),
        surfaceVariant = Color(0xFFF5DED4),
        onSurfaceVariant = Color(0xFF53433C),
        outline = Color(0xFF85736B)
    )

    val OrangeDarkScheme = darkColorScheme(
        primary = Color(0xFFFFB74D),
        onPrimary = Color(0xFF4E1D00),
        primaryContainer = Color(0xFF702C00),
        onPrimaryContainer = Color(0xFFFFDCC2),
        secondary = Color(0xFFFF8A65),
        onSecondary = Color(0xFF4A1000),
        secondaryContainer = Color(0xFF701C00),
        onSecondaryContainer = Color(0xFFFFDBD1),
        background = Color(0xFF18120F),
        onBackground = Color(0xFFECE0DA),
        surface = Color(0xFF201A17),
        onSurface = Color(0xFFECE0DA),
        surfaceVariant = Color(0xFF53433C),
        onSurfaceVariant = Color(0xFFD8C2B9),
        outline = Color(0xFFA08C84)
    )

    // RED
    val RedLightScheme = lightColorScheme(
        primary = Color(0xFFC62828),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFCDD2),
        onPrimaryContainer = Color(0xFF3B0000),
        secondary = Color(0xFFB71C1C),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFEBEE),
        onSecondaryContainer = Color(0xFF410002),
        background = Color(0xFFFFFBFB),
        onBackground = Color(0xFF201A1A),
        surface = Color.White,
        onSurface = Color(0xFF201A1A),
        surfaceVariant = Color(0xFFF5DDDC),
        onSurfaceVariant = Color(0xFF534343),
        outline = Color(0xFF857373)
    )

    val RedDarkScheme = darkColorScheme(
        primary = Color(0xFFEF9A9A),
        onPrimary = Color(0xFF490000),
        primaryContainer = Color(0xFF680003),
        onPrimaryContainer = Color(0xFFFFDAD6),
        secondary = Color(0xFFE57373),
        onSecondary = Color(0xFF440001),
        secondaryContainer = Color(0xFF650004),
        onSecondaryContainer = Color(0xFFFFDAD8),
        background = Color(0xFF181212),
        onBackground = Color(0xFFECE0E0),
        surface = Color(0xFF201A1A),
        onSurface = Color(0xFFECE0E0),
        surfaceVariant = Color(0xFF534343),
        onSurfaceVariant = Color(0xFFD8C2C1),
        outline = Color(0xFFA08C8C)
    )

    // PINK
    val PinkLightScheme = lightColorScheme(
        primary = Color(0xFFD81B60),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFF8BBD0),
        onPrimaryContainer = Color(0xFF3A0017),
        secondary = Color(0xFFAD1457),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFCE4EC),
        onSecondaryContainer = Color(0xFF480020),
        background = Color(0xFFFFFBFB),
        onBackground = Color(0xFF201A1C),
        surface = Color.White,
        onSurface = Color(0xFF201A1C),
        surfaceVariant = Color(0xFFF2DDE2),
        onSurfaceVariant = Color(0xFF514347),
        outline = Color(0xFF837377)
    )

    val PinkDarkScheme = darkColorScheme(
        primary = Color(0xFFF48FB1),
        onPrimary = Color(0xFF4D0024),
        primaryContainer = Color(0xFF700037),
        onPrimaryContainer = Color(0xFFFFD8E4),
        secondary = Color(0xFFEC407A),
        onSecondary = Color(0xFF44001F),
        secondaryContainer = Color(0xFF640030),
        onSecondaryContainer = Color(0xFFFFD9E2),
        background = Color(0xFF171214),
        onBackground = Color(0xFFECE0E2),
        surface = Color(0xFF201A1C),
        onSurface = Color(0xFFECE0E2),
        surfaceVariant = Color(0xFF514347),
        onSurfaceVariant = Color(0xFFD5C2C6),
        outline = Color(0xFF9E8C90)
    )
}
