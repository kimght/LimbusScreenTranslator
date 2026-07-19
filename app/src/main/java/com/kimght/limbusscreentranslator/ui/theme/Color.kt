package com.kimght.limbusscreentranslator.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val Limbus50 = Color(0xFFFBF8EB)
val Limbus100 = Color(0xFFF7EDCA)
val Limbus200 = Color(0xFFF0DA98)
val Limbus300 = Color(0xFFE8BD5C)
val Limbus400 = Color(0xFFDFA330)
val Limbus500 = Color(0xFFCF8D23)
val Limbus600 = Color(0xFFB36D1B)
val Limbus700 = Color(0xFF94511A)
val Limbus800 = Color(0xFF773F1C)
val Limbus900 = Color(0xFF66361D)
val Limbus950 = Color(0xFF3B1B0D)

val BgBackground = Color(0xFF231C20)

val Danger = Color(0xFFCF3723)

val DangerBright = Color(0xFFE57F6E)

val Hairline = Limbus600.copy(alpha = 0.30f)

val HairlineStrong = Limbus600.copy(alpha = 0.50f)

val InsetBg = Color(0x40000000)

val LimbusDarkColorScheme = darkColorScheme(
    primary = Limbus500,
    onPrimary = BgBackground,
    primaryContainer = Limbus600,
    onPrimaryContainer = Limbus50,
    secondary = Limbus400,
    onSecondary = BgBackground,
    tertiary = Limbus300,
    onTertiary = BgBackground,
    background = BgBackground,
    onBackground = Limbus200,
    surface = BgBackground,
    onSurface = Limbus200,
    surfaceVariant = BgBackground,
    onSurfaceVariant = Limbus200,
    surfaceContainer = BgBackground,
    surfaceContainerHigh = BgBackground,
    outline = Limbus500,
    outlineVariant = Hairline,
    error = Danger,
    onError = Limbus50,
)
