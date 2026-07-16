package com.kimght.LimbusScreenTranslator.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kimght.LimbusScreenTranslator.R
import com.kimght.LimbusScreenTranslator.ui.theme.BgBackground
import com.kimght.LimbusScreenTranslator.ui.theme.Danger
import com.kimght.LimbusScreenTranslator.ui.theme.DangerBright
import com.kimght.LimbusScreenTranslator.ui.theme.Hairline
import com.kimght.LimbusScreenTranslator.ui.theme.HairlineStrong
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus100
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus50
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus500
import com.kimght.LimbusScreenTranslator.ui.theme.MonoFontFamily

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BgBackground)
                .border(1.dp, HairlineStrong, RoundedCornerShape(12.dp))
                .padding(20.dp),
        ) {
            Text(
                text = title,
                color = Limbus100,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Text(
                text = message,
                color = Limbus500,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Limbus50.copy(alpha = 0.02f))
                        .border(1.dp, Hairline, RoundedCornerShape(3.dp))
                        .clickableEnabled(true) { onDismiss() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        color = Limbus500,
                        fontFamily = MonoFontFamily,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp,
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Danger.copy(alpha = 0.14f))
                        .border(1.dp, Danger.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
                        .clickableEnabled(true) { onConfirm() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = confirmLabel,
                        color = DangerBright,
                        fontFamily = MonoFontFamily,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp,
                    )
                }
            }
        }
    }
}
