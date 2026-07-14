package com.kimght.LimbusScreenTranslator.core.designsystem

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kimght.LimbusScreenTranslator.ui.theme.Hairline
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus300
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus500
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus600
import com.kimght.LimbusScreenTranslator.ui.theme.MonoFontFamily

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text.uppercase(),
            color = Limbus600,
            fontFamily = MonoFontFamily,
            fontSize = 10.sp,
            letterSpacing = 2.2.sp,
            fontWeight = FontWeight.Medium,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                color = Limbus600,
                fontFamily = MonoFontFamily,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private object FlagAssets {
    private const val DIR = "flags"

    @Volatile
    private var available: Set<String>? = null

    fun contains(context: Context, code: String): Boolean {
        val flags = available ?: synchronized(this) {
            available ?: context.assets.list(DIR)
                .orEmpty()
                .filter { it.endsWith(".webp") }
                .map { it.removeSuffix(".webp") }
                .toSet()
                .also { available = it }
        }
        return code in flags
    }

    fun uri(code: String): String = "file:///android_asset/$DIR/$code.webp"
}

@Composable
fun FlagChip(
    flag: String,
    modifier: Modifier = Modifier,
    width: Dp = 46.dp,
    height: Dp = 34.dp,
    fontSize: TextUnit = 12.sp,
) {
    val context = LocalContext.current
    val code = flag.trim().substringBeforeLast(".").uppercase()
    val hasImage = remember(code) { FlagAssets.contains(context, code) }
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .size(width, height)
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        if (hasImage) {
            AsyncImage(
                model = FlagAssets.uri(code),
                contentDescription = flag,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Text(
                text = flag,
                color = Limbus300,
                fontFamily = MonoFontFamily,
                fontSize = fontSize,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable (Color) -> Unit)? = null,
    accent: Color = Limbus500,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val content = when {
        !enabled -> accent.copy(alpha = 0.45f)
        pressed -> Limbus300
        else -> accent
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, content.copy(alpha = content.alpha * 0.55f), RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) leading(content)
        Text(
            text = text.uppercase(),
            color = content,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = 1.3.sp,
        )
    }
}

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    foreground: Color = Limbus500,
    border: Color = Hairline,
    background: Color = Color.Transparent,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(3.dp))
            .clickableEnabled(true, onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = foreground,
            fontFamily = MonoFontFamily,
            fontSize = 12.sp,
            letterSpacing = 1.0.sp,
        )
    }
}

@Composable
fun InstallProgressRow(
    stageLabel: String,
    percent: Int,
    modifier: Modifier = Modifier,
    barHeight: Dp = 5.dp,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stageLabel,
                color = Limbus500,
                fontFamily = MonoFontFamily,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = "$percent%",
                color = Limbus500,
                fontFamily = MonoFontFamily,
                fontSize = 9.sp,
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(Limbus600.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                    .height(barHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            0.0f to Limbus600,
                            1.0f to Limbus300,
                        ),
                    ),
            )
        }
    }
}
