package com.kimght.limbusscreentranslator.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.X
import com.kimght.limbusscreentranslator.R
import com.kimght.limbusscreentranslator.core.designsystem.FlagChip
import com.kimght.limbusscreentranslator.core.designsystem.GoldButton
import com.kimght.limbusscreentranslator.core.designsystem.InstallProgressRow
import com.kimght.limbusscreentranslator.core.designsystem.MarkdownChangelog
import com.kimght.limbusscreentranslator.core.designsystem.SectionLabel
import com.kimght.limbusscreentranslator.core.designsystem.StatusBadge
import com.kimght.limbusscreentranslator.core.designsystem.clickableEnabled
import com.kimght.limbusscreentranslator.domain.model.LocalizationStatus
import com.kimght.limbusscreentranslator.feature.library.formatSize
import com.kimght.limbusscreentranslator.ui.theme.Danger
import com.kimght.limbusscreentranslator.ui.theme.DangerBright
import com.kimght.limbusscreentranslator.ui.theme.Hairline
import com.kimght.limbusscreentranslator.ui.theme.InsetBg
import com.kimght.limbusscreentranslator.ui.theme.Limbus100
import com.kimght.limbusscreentranslator.ui.theme.Limbus300
import com.kimght.limbusscreentranslator.ui.theme.Limbus400
import com.kimght.limbusscreentranslator.ui.theme.Limbus500
import com.kimght.limbusscreentranslator.ui.theme.Limbus600
import com.kimght.limbusscreentranslator.ui.theme.MonoFontFamily

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onUninstalled: () -> Unit,
    onOpenOverlay: () -> Unit,
    onCloseOverlay: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DetailContent(
        state = state,
        onBack = onBack,
        onInstall = viewModel::install,
        onSetActive = viewModel::setActive,
        onUninstall = { viewModel.uninstall(onComplete = onUninstalled) },
        onOpenOverlay = onOpenOverlay,
        onCloseOverlay = onCloseOverlay,
        modifier = modifier,
    )
}

@Composable
private fun DetailContent(
    state: DetailUiState,
    onBack: () -> Unit,
    onInstall: () -> Unit,
    onSetActive: () -> Unit,
    onUninstall: () -> Unit,
    onOpenOverlay: () -> Unit,
    onCloseOverlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 28.dp),
    ) {
        Row(
            modifier = Modifier
                .clickableEnabled(true, onBack)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Lucide.ChevronLeft,
                contentDescription = stringResource(R.string.detail_back),
                modifier = Modifier.size(18.dp),
                tint = Limbus500,
            )
            Text(
                text = stringResource(R.string.detail_library),
                color = Limbus500,
                fontFamily = MonoFontFamily,
                fontSize = 11.sp,
                letterSpacing = 1.0.sp,
            )
        }

        val loc = state.localization
        when {
            state.notFound -> Text(
                text = stringResource(R.string.detail_not_found),
                color = Limbus500,
                fontSize = 14.sp,
                modifier = Modifier.padding(20.dp),
            )

            loc == null -> Text(
                text = stringResource(R.string.detail_loading),
                color = Limbus500,
                fontSize = 13.sp,
                modifier = Modifier.padding(20.dp),
            )

            else -> Column(Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    FlagChip(loc.flag, width = 56.dp, height = 42.dp, fontSize = 14.sp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = loc.name,
                            color = Limbus100,
                            fontWeight = FontWeight.Bold,
                            fontSize = 25.sp,
                        )
                        Text(
                            text = stringResource(
                                R.string.detail_by_authors,
                                loc.authors.joinToString(", "),
                            ),
                            color = Limbus500,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    StatusBadge(state.status)
                }

                Spacer(Modifier.size(16.dp))
                MetadataGrid(version = loc.version, size = formatSize(loc.sizeBytes))

                Spacer(Modifier.size(14.dp))
                ActionRow(
                    status = state.status,
                    installing = state.installPercent != null,
                    overlayRunning = state.overlayRunning,
                    onInstall = onInstall,
                    onSetActive = onSetActive,
                    onOpenOverlay = onOpenOverlay,
                    onCloseOverlay = onCloseOverlay,
                )
                val canUninstall = state.status in setOf(
                    LocalizationStatus.INSTALLED,
                    LocalizationStatus.ACTIVE,
                    LocalizationStatus.UPDATE_AVAILABLE,
                ) && state.installPercent == null
                if (canUninstall) {
                    Spacer(Modifier.size(8.dp))
                    UninstallButton(onUninstall)
                }
                if (state.installPercent != null) {
                    Spacer(Modifier.size(8.dp))
                    InstallProgressRow(
                        stageLabel = stringResource(state.installStage),
                        percent = state.installPercent,
                        barHeight = 6.dp,
                    )
                }

                if (loc.description.isNotBlank()) {
                    Spacer(Modifier.size(20.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(InsetBg)
                            .border(1.dp, Hairline, RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                    ) {
                        SectionLabel(stringResource(R.string.detail_changelog))
                        Spacer(Modifier.size(11.dp))
                        MarkdownChangelog(loc.description)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataGrid(version: String, size: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Hairline, RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        MetaCell(stringResource(R.string.detail_version), version, Modifier.weight(1f))
        MetaCell(stringResource(R.string.detail_size), size, Modifier.weight(1f))
    }
}

@Composable
private fun MetaCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(InsetBg)
            .padding(horizontal = 13.dp, vertical = 10.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = Limbus600,
            fontFamily = MonoFontFamily,
            fontSize = 9.sp,
            letterSpacing = 1.0.sp,
        )
        Text(
            text = value,
            color = Limbus100,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun ActionRow(
    status: LocalizationStatus,
    installing: Boolean,
    overlayRunning: Boolean,
    onInstall: () -> Unit,
    onSetActive: () -> Unit,
    onOpenOverlay: () -> Unit,
    onCloseOverlay: () -> Unit,
) {
    if (installing) return
    when (status) {
        LocalizationStatus.NOT_INSTALLED ->
            GoldButton(text = stringResource(R.string.detail_install), onClick = onInstall)

        LocalizationStatus.UPDATE_AVAILABLE ->
            GoldButton(
                text = stringResource(R.string.detail_update_now),
                onClick = onInstall,
                accent = Limbus400,
            )

        LocalizationStatus.INSTALLED ->
            GoldButton(
                text = stringResource(R.string.detail_set_active),
                onClick = onSetActive,
                accent = Limbus300,
            )

        LocalizationStatus.ACTIVE ->
            GoldButton(
                text = stringResource(
                    if (overlayRunning) R.string.home_close_overlay else R.string.home_open_overlay,
                ),
                onClick = if (overlayRunning) onCloseOverlay else onOpenOverlay,
                leading = { color ->
                    Icon(
                        imageVector = if (overlayRunning) Lucide.X else Lucide.Play,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = color,
                    )
                },
            )

        LocalizationStatus.INSTALLING -> Unit
    }
}

@Composable
private fun UninstallButton(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(Danger.copy(alpha = 0.14f))
            .border(1.dp, Danger.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
            .clickableEnabled(true, onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.detail_uninstall),
            color = DangerBright,
            fontFamily = MonoFontFamily,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp,
        )
    }
}
