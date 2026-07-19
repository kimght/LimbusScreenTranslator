package com.kimght.limbusscreentranslator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun LimbusScreenTranslatorTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LimbusDarkColorScheme,
        typography = LimbusTypography,
        content = content,
    )
}
