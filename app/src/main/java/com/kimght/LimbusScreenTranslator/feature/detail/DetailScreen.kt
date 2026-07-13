package com.kimght.LimbusScreenTranslator.feature.detail

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.kimght.LimbusScreenTranslator.core.designsystem.FlagChip
import com.kimght.LimbusScreenTranslator.core.designsystem.GoldButton
import com.kimght.LimbusScreenTranslator.core.designsystem.InstallProgressRow
import com.kimght.LimbusScreenTranslator.core.designsystem.MarkdownChangelog
import com.kimght.LimbusScreenTranslator.core.designsystem.SectionLabel
import com.kimght.LimbusScreenTranslator.core.designsystem.StatusBadge
import com.kimght.LimbusScreenTranslator.core.designsystem.clickableEnabled
import com.kimght.LimbusScreenTranslator.domain.model.LocalizationStatus
import com.kimght.LimbusScreenTranslator.feature.library.formatSize
import com.kimght.LimbusScreenTranslator.ui.theme.Danger
import com.kimght.LimbusScreenTranslator.ui.theme.DangerBright
import com.kimght.LimbusScreenTranslator.ui.theme.Hairline
import com.kimght.LimbusScreenTranslator.ui.theme.InsetBg
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus100
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus300
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus400
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus500
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus600
import com.kimght.LimbusScreenTranslator.ui.theme.MonoFontFamily

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onUninstalled: () -> Unit,
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
                contentDescription = "Back",
                modifier = Modifier.size(18.dp),
                tint = Limbus500,
            )
            Text(
                text = "LIBRARY",
                color = Limbus500,
                fontFamily = MonoFontFamily,
                fontSize = 11.sp,
                letterSpacing = 1.0.sp,
            )
        }

        val loc = state.localization
        when {
            state.notFound -> Text(
                text = "This localization is no longer available from its source.",
                color = Limbus500,
                fontSize = 14.sp,
                modifier = Modifier.padding(20.dp),
            )

            loc == null -> Text(
                text = "Loading…",
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
                            text = "by ${loc.authors.joinToString(", ")}",
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
                    onInstall = onInstall,
                    onSetActive = onSetActive,
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
                        stageLabel = state.installStage,
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
                        SectionLabel("Changelog")
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
        MetaCell("Version", version, Modifier.weight(1f))
        MetaCell("Size", size, Modifier.weight(1f))
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
    onInstall: () -> Unit,
    onSetActive: () -> Unit,
) {
    if (installing) return
    when (status) {
        LocalizationStatus.NOT_INSTALLED ->
            GoldButton(text = "Install", onClick = onInstall)

        LocalizationStatus.UPDATE_AVAILABLE ->
            GoldButton(text = "Update now", onClick = onInstall, accent = Limbus400)

        LocalizationStatus.INSTALLED ->
            GoldButton(
                text = "Set active",
                onClick = onSetActive,
                accent = Limbus300,
            )

        LocalizationStatus.ACTIVE,
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
            text = "UNINSTALL",
            color = DangerBright,
            fontFamily = MonoFontFamily,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp,
        )
    }
}
