package com.kimght.LimbusScreenTranslator.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kimght.LimbusScreenTranslator.R
import com.kimght.LimbusScreenTranslator.domain.model.LocalizationStatus
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus200
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus300
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus400
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus50
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus500
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus600
import com.kimght.LimbusScreenTranslator.ui.theme.MonoFontFamily

@Composable
fun StatusBadge(
    status: LocalizationStatus,
    modifier: Modifier = Modifier,
) {
    val spec = status.badgeSpec()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(spec.background)
            .border(1.dp, spec.border, RoundedCornerShape(2.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (spec.dot != null) {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(spec.dot),
            )
        }
        Text(
            text = stringResource(spec.label),
            color = spec.foreground,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 0.8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class BadgeSpec(
    @StringRes val label: Int,
    val foreground: Color,
    val background: Color,
    val border: Color,
    val dot: Color? = null,
)

private fun LocalizationStatus.badgeSpec(): BadgeSpec = when (this) {
    LocalizationStatus.NOT_INSTALLED -> BadgeSpec(
        label = R.string.badge_not_installed,
        foreground = Limbus500,
        background = Limbus50.copy(alpha = 0.03f),
        border = Limbus600.copy(alpha = 0.30f),
    )

    LocalizationStatus.INSTALLED -> BadgeSpec(
        label = R.string.badge_installed,
        foreground = Limbus200,
        background = Limbus300.copy(alpha = 0.06f),
        border = Limbus300.copy(alpha = 0.28f),
    )

    LocalizationStatus.ACTIVE -> BadgeSpec(
        label = R.string.badge_active,
        foreground = Limbus300,
        background = Limbus300.copy(alpha = 0.12f),
        border = Limbus300.copy(alpha = 0.50f),
    )

    LocalizationStatus.UPDATE_AVAILABLE -> BadgeSpec(
        label = R.string.badge_update,
        foreground = Limbus400,
        background = Limbus400.copy(alpha = 0.12f),
        border = Limbus400.copy(alpha = 0.55f),
        dot = Limbus400,
    )

    LocalizationStatus.INSTALLING -> BadgeSpec(
        label = R.string.badge_installing,
        foreground = Limbus300,
        background = Limbus300.copy(alpha = 0.10f),
        border = Limbus300.copy(alpha = 0.40f),
    )
}
