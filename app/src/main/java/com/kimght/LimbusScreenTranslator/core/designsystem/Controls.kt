package com.kimght.LimbusScreenTranslator.core.designsystem

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus100
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus50
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus500
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus600

fun Modifier.clickableEnabled(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) this.clickable(onClick = onClick) else this

@Composable
fun GoldSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val knobX by animateDpAsState(if (checked) 22.dp else 2.dp, label = "knobX")
    val trackBg = if (checked) Limbus500.copy(alpha = 0.55f) else Limbus50.copy(alpha = 0.06f)
    val trackBorder = if (checked) Limbus500 else Limbus600.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .size(44.dp, 24.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(trackBg)
            .border(1.dp, trackBorder, RoundedCornerShape(13.dp))
            .clickable { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobX, y = 2.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Limbus100),
        )
    }
}
