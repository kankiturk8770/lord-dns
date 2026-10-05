package com.lord.dns.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

val PingGood = Color(0xFF1FAF6E)
val PingMid = Color(0xFFDB9A14)
val PingBad = Color(0xFFE5484D)

enum class AppTheme(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    SURFSHARK("Surfshark"),
    PROTON("Proton"),
    MIDNIGHT("Midnight"),
    SUNSET("Sunset");

    companion object {
        fun from(name: String?): AppTheme = entries.firstOrNull { it.name == name } ?: LIGHT
    }
}

private fun light(primary: Color, bg: Color, surface: Color, variant: Color, text: Color, muted: Color, line: Color) =
    lightColorScheme(
        primary = primary, onPrimary = Color(0xFFFDFEFE),
        primaryContainer = primary.copy(alpha = 0.14f).compositeOver(surface), onPrimaryContainer = primary,
        secondary = primary, onSecondary = Color(0xFFFDFEFE),
        secondaryContainer = primary.copy(alpha = 0.14f).compositeOver(surface), onSecondaryContainer = text,
        background = bg, onBackground = text,
        surface = surface, onSurface = text,
        surfaceVariant = variant, onSurfaceVariant = muted,
        surfaceContainerLowest = surface, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = variant, surfaceContainerHighest = variant,
        outline = line, outlineVariant = line,
    )

private fun dark(primary: Color, onPrimary: Color, bg: Color, surface: Color, variant: Color, text: Color, muted: Color, line: Color) =
    darkColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primary.copy(alpha = 0.20f).compositeOver(surface), onPrimaryContainer = primary,
        secondary = primary, onSecondary = onPrimary,
        secondaryContainer = primary.copy(alpha = 0.20f).compositeOver(surface), onSecondaryContainer = text,
        background = bg, onBackground = text,
        surface = surface, onSurface = text,
        surfaceVariant = variant, onSurfaceVariant = muted,
        surfaceContainerLowest = bg, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = variant, surfaceContainerHighest = variant,
        outline = line, outlineVariant = line,
    )

private val Light = light(
    primary = Color(0xFF1A9AA0), bg = Color(0xFFF2F7F8), surface = Color(0xFFFCFEFE),
    variant = Color(0xFFE6EFF1), text = Color(0xFF16242A), muted = Color(0xFF66797F), line = Color(0xFFDCE6E9),
)
private val Dark = dark(
    primary = Color(0xFF3CC9C6), onPrimary = Color(0xFF062A2C), bg = Color(0xFF0E1316), surface = Color(0xFF161D21),
    variant = Color(0xFF1F282D), text = Color(0xFFE6EEF0), muted = Color(0xFF8E9FA5), line = Color(0xFF263136),
)
private val Surfshark = dark(
    primary = Color(0xFF1EC8BE), onPrimary = Color(0xFF04201F), bg = Color(0xFF0B1A22), surface = Color(0xFF11242E),
    variant = Color(0xFF173039), text = Color(0xFFE3F2F3), muted = Color(0xFF86A4AC), line = Color(0xFF1D3843),
)
private val Proton = dark(
    primary = Color(0xFF8B6CFF), onPrimary = Color(0xFFF7F4FF), bg = Color(0xFF17151F), surface = Color(0xFF201D2B),
    variant = Color(0xFF2A2638), text = Color(0xFFECE9F6), muted = Color(0xFF9D97B3), line = Color(0xFF332E45),
)
private val Midnight = dark(
    primary = Color(0xFF5E8EFF), onPrimary = Color(0xFFF3F6FF), bg = Color(0xFF0A0F1E), surface = Color(0xFF111833),
    variant = Color(0xFF18213F), text = Color(0xFFE4E9F7), muted = Color(0xFF8C97B8), line = Color(0xFF1F2A4C),
)
private val Sunset = light(
    primary = Color(0xFFE2603E), bg = Color(0xFFFBF4F0), surface = Color(0xFFFFFCFA),
    variant = Color(0xFFF4E8E2), text = Color(0xFF2B1D19), muted = Color(0xFF85706A), line = Color(0xFFEEDFD8),
)

fun AppTheme.scheme(systemDark: Boolean): ColorScheme = when (this) {
    AppTheme.SYSTEM -> if (systemDark) Dark else Light
    AppTheme.LIGHT -> Light
    AppTheme.DARK -> Dark
    AppTheme.SURFSHARK -> Surfshark
    AppTheme.PROTON -> Proton
    AppTheme.MIDNIGHT -> Midnight
    AppTheme.SUNSET -> Sunset
}

@Composable
fun LordTheme(scheme: ColorScheme, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
