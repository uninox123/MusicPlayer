package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Parse Hex Color String safely
fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF3D69FF) // fallback electric blue
    }
}

@Composable
fun NovaMusicTheme(
    themeMode: String = "dark", // "light", "dark", "system", "amoled"
    primaryColorHex: String = "#3D69FF",
    useMaterialYou: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark", "amoled" -> true
        "light" -> false
        else -> systemDark
    }

    val primaryColor = parseHexColor(primaryColorHex)

    // Dynamic light/dark palettes built from primary color
    val colorScheme: ColorScheme = when {
        useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        isDark -> {
            // Build Dark/AMOLED Theme
            val isAmoled = themeMode == "amoled"
            darkColorScheme(
                primary = primaryColor,
                secondary = primaryColor.copy(alpha = 0.8f),
                tertiary = primaryColor.copy(alpha = 0.6f),
                background = if (isAmoled) Color.Black else Color(0xFF0D0E15),
                surface = if (isAmoled) Color(0xFF101118) else Color(0xFF161824),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFFE2E4F0),
                onSurface = Color(0xFFE2E4F0),
                surfaceVariant = if (isAmoled) Color(0xFF181B26) else Color(0xFF222538)
            )
        }
        else -> {
            // Build Light Theme
            lightColorScheme(
                primary = primaryColor,
                secondary = primaryColor.copy(alpha = 0.8f),
                tertiary = primaryColor.copy(alpha = 0.6f),
                background = Color(0xFFF7F8FC),
                surface = Color.White,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF1A1C24),
                onSurface = Color(0xFF1A1C24),
                surfaceVariant = Color(0xFFECEFF5)
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
