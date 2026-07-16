package com.kimght.LimbusScreenTranslator.core.i18n

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import java.util.Locale

fun Context.localizedTo(language: String): Context {
    val locale = Locale.forLanguageTag(language)
    val config = Configuration(resources.configuration).apply { setLocale(locale) }
    return createConfigurationContext(config)
}

fun Context.localizedWrapper(language: String): ContextWrapper {
    val configured = localizedTo(language)
    return object : ContextWrapper(this) {
        override fun getResources(): Resources = configured.resources
        override fun getAssets(): AssetManager = configured.assets
    }
}

@Composable
fun ProvideUiLanguage(language: String, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val localized = remember(base, language) { base.localizedWrapper(language) }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        LocalResources provides localized.resources,
        content = content,
    )
}
