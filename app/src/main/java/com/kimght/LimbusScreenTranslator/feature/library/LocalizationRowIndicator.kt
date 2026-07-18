package com.kimght.LimbusScreenTranslator.feature.library

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CheckCheck
import com.composables.icons.lucide.FolderSync
import com.composables.icons.lucide.Lucide
import com.kimght.LimbusScreenTranslator.R
import com.kimght.LimbusScreenTranslator.domain.model.LocalizationStatus
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus300
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus400
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus500

internal data class RowIndicator(
    @StringRes val label: Int,
    val icon: ImageVector?,
    val tint: Color,
    val spinner: Boolean = false,
)

internal fun LocalizationStatus.rowIndicator(): RowIndicator? = when (this) {
    LocalizationStatus.NOT_INSTALLED -> null

    LocalizationStatus.INSTALLED ->
        RowIndicator(R.string.badge_installed, Lucide.Check, Limbus500)

    LocalizationStatus.ACTIVE ->
        RowIndicator(R.string.badge_active, Lucide.CheckCheck, Limbus300)

    LocalizationStatus.UPDATE_AVAILABLE ->
        RowIndicator(R.string.badge_update, Lucide.FolderSync, Limbus400)

    LocalizationStatus.INSTALLING ->
        RowIndicator(R.string.badge_installing, icon = null, tint = Limbus300, spinner = true)
}
