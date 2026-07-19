package com.kimght.limbusscreentranslator.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.kimght.limbusscreentranslator.R
import com.kimght.limbusscreentranslator.core.designsystem.ConfirmDialog
import com.kimght.limbusscreentranslator.core.designsystem.SectionLabel
import com.kimght.limbusscreentranslator.core.designsystem.clickableEnabled
import com.kimght.limbusscreentranslator.data.datastore.Settings
import com.kimght.limbusscreentranslator.domain.model.Source
import com.kimght.limbusscreentranslator.ui.theme.BgBackground
import com.kimght.limbusscreentranslator.ui.theme.Danger
import com.kimght.limbusscreentranslator.ui.theme.DangerBright
import com.kimght.limbusscreentranslator.ui.theme.Hairline
import com.kimght.limbusscreentranslator.ui.theme.InsetBg
import com.kimght.limbusscreentranslator.ui.theme.Limbus100
import com.kimght.limbusscreentranslator.ui.theme.Limbus200
import com.kimght.limbusscreentranslator.ui.theme.Limbus300
import com.kimght.limbusscreentranslator.ui.theme.Limbus50
import com.kimght.limbusscreentranslator.ui.theme.Limbus500
import com.kimght.limbusscreentranslator.ui.theme.Limbus600
import com.kimght.limbusscreentranslator.ui.theme.MonoFontFamily

@Composable
fun SettingsScreen(
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    androidx.compose.runtime.LaunchedEffect(viewModel, resources) {
        viewModel.messages.collect { msg ->
            onMessage(resources.getString(msg.id, *msg.args.toTypedArray()))
        }
    }
    SettingsContent(
        state = state,
        onOpacity = viewModel::setOpacityPercent,
        onUiLanguage = viewModel::setUiLanguage,
        onAddSource = viewModel::addSource,
        onRemoveSource = viewModel::removeSource,
        onResetEverything = viewModel::resetEverything,
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onOpacity: (Int) -> Unit,
    onUiLanguage: (String) -> Unit,
    onAddSource: (String, String) -> Unit,
    onRemoveSource: (String) -> Unit,
    onResetEverything: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp, bottom = 28.dp),
    ) {
        SectionLabel(stringResource(R.string.settings_overlay))
        Spacer(Modifier.size(12.dp))
        OpacityCard(percent = state.opacityPercent, opacity = state.opacity, onOpacity = onOpacity)

        Spacer(Modifier.size(20.dp))
        SectionLabel(stringResource(R.string.settings_interface_language))
        Spacer(Modifier.size(10.dp))
        LanguageGrid(selected = state.uiLanguage, onSelect = onUiLanguage)

        Spacer(Modifier.size(20.dp))
        SectionLabel(stringResource(R.string.settings_sources))
        Spacer(Modifier.size(10.dp))
        SourceManager(
            sources = state.sources,
            onAdd = onAddSource,
            onRemove = onRemoveSource,
        )

        Spacer(Modifier.size(20.dp))
        SectionLabel(stringResource(R.string.settings_reset_section))
        Spacer(Modifier.size(10.dp))
        var confirmingReset by remember { mutableStateOf(false) }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp))
                .background(Danger.copy(alpha = 0.10f))
                .border(1.dp, Danger.copy(alpha = 0.32f), RoundedCornerShape(3.dp))
                .clickableEnabled(true) { confirmingReset = true }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.settings_reset_to_defaults),
                color = DangerBright,
                fontFamily = MonoFontFamily,
                fontSize = 12.sp,
                letterSpacing = 1.0.sp,
            )
        }
        if (confirmingReset) {
            ConfirmDialog(
                title = stringResource(R.string.settings_reset_title),
                message = stringResource(R.string.settings_reset_message),
                confirmLabel = stringResource(R.string.settings_reset_confirm),
                onConfirm = {
                    onResetEverything()
                    confirmingReset = false
                },
                onDismiss = { confirmingReset = false },
            )
        }
    }
}

@Composable
private fun OpacityCard(percent: Int, opacity: Float, onOpacity: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InsetBg)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(R.string.settings_background_opacity),
                color = Limbus200,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                letterSpacing = 1.0.sp,
            )
            Text(
                text = "$percent%",
                color = Limbus300,
                fontFamily = MonoFontFamily,
                fontSize = 13.sp
            )
        }
        Slider(
            value = percent.toFloat(),
            onValueChange = { onOpacity(it.toInt()) },
            valueRange = (Settings.MIN_OPACITY * 100)..(Settings.MAX_OPACITY * 100),
            colors = SliderDefaults.colors(
                thumbColor = Limbus500,
                activeTrackColor = Limbus500,
                inactiveTrackColor = Limbus600.copy(alpha = 0.25f),
            ),
        )
        Column(
            Modifier
                .padding(top = 13.dp)
                .fillMaxWidth()
                .heightIn(min = 94.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(InsetBg)
                .padding(start = 12.dp, end = 12.dp, top = 9.dp, bottom = 13.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.settings_preview),
                color = Limbus600,
                fontFamily = MonoFontFamily,
                fontSize = 8.sp,
                letterSpacing = 1.0.sp,
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .alpha(opacity)
                    .background(BgBackground.copy(alpha = 0.92f))
                    .border(1.dp, Limbus300.copy(alpha = 0.34f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 13.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_preview_speaker),
                    color = Limbus300,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp,
                )
                Text(
                    text = stringResource(R.string.settings_preview_line),
                    color = Limbus100,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun LanguageGrid(selected: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        SettingsViewModel.UI_LANGUAGES.forEach { lang ->
            val isSelected = lang.code == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (isSelected) Limbus300.copy(alpha = 0.12f) else Limbus50.copy(
                            alpha = 0.02f
                        )
                    )
                    .border(
                        1.dp,
                        if (isSelected) Limbus300.copy(alpha = 0.55f) else Limbus600.copy(alpha = 0.30f),
                        RoundedCornerShape(3.dp),
                    )
                    .clickableEnabled(true) { onSelect(lang.code) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text(
                    text = lang.label,
                    color = if (isSelected) Limbus300 else Limbus500,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun SourceManager(
    sources: List<Source>,
    onAdd: (String, String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newHost by remember { mutableStateOf("") }
    var pendingRemoval by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sources.forEach { source ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Limbus50.copy(alpha = 0.02f))
                        .border(1.dp, Limbus600.copy(alpha = 0.30f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = source.name,
                            color = Limbus200,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = source.host,
                            color = Limbus600,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Box(
                    Modifier
                        .width(42.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Danger.copy(alpha = 0.10f))
                        .border(1.dp, Danger.copy(alpha = 0.32f), RoundedCornerShape(3.dp))
                        .clickableEnabled(true) { pendingRemoval = source.name },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = DangerBright,
                    )
                }
            }
        }

        if (adding) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Limbus300.copy(alpha = 0.05f))
                    .border(1.dp, Limbus300.copy(alpha = 0.30f), RoundedCornerShape(3.dp))
                    .padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SourceInput(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = stringResource(R.string.settings_source_name_placeholder)
                )
                SourceInput(
                    value = newHost,
                    onValueChange = { newHost = it },
                    placeholder = stringResource(R.string.settings_source_host_placeholder),
                    mono = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Limbus500.copy(alpha = 0.18f))
                            .border(1.dp, Limbus500, RoundedCornerShape(3.dp))
                            .clickableEnabled(newName.isNotBlank()) {
                                onAdd(newName, newHost)
                                adding = false
                                newName = ""
                                newHost = ""
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_add_source),
                            color = Limbus300,
                            fontFamily = MonoFontFamily,
                            fontSize = 12.sp,
                            letterSpacing = 0.8.sp,
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Limbus50.copy(alpha = 0.02f))
                            .border(1.dp, Hairline, RoundedCornerShape(3.dp))
                            .clickableEnabled(true) {
                                adding = false
                                newName = ""
                                newHost = ""
                            }
                            .padding(horizontal = 16.dp, vertical = 11.dp),
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
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .border(1.dp, Hairline, RoundedCornerShape(3.dp))
                    .clickableEnabled(true) { adding = true }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.settings_add_source_button),
                    color = Limbus500,
                    fontFamily = MonoFontFamily,
                    fontSize = 12.sp,
                    letterSpacing = 1.0.sp,
                )
            }
        }

        pendingRemoval?.let { name ->
            ConfirmDialog(
                title = stringResource(R.string.settings_remove_source_title, name),
                message = stringResource(R.string.settings_remove_source_message),
                confirmLabel = stringResource(R.string.settings_remove),
                onConfirm = {
                    onRemove(name)
                    pendingRemoval = null
                },
                onDismiss = { pendingRemoval = null },
            )
        }
    }
}

@Composable
private fun SourceInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    mono: Boolean = false,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0x4D000000))
            .border(1.dp, Hairline, RoundedCornerShape(3.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = Limbus500,
                fontFamily = if (mono) MonoFontFamily else androidx.compose.ui.text.font.FontFamily.Default,
                fontSize = if (mono) 12.sp else 14.sp,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = if (mono) Limbus200 else Limbus100,
                fontFamily = if (mono) MonoFontFamily else androidx.compose.ui.text.font.FontFamily.Default,
                fontSize = if (mono) 12.sp else 14.sp,
            ),
            cursorBrush = SolidColor(Limbus300),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
