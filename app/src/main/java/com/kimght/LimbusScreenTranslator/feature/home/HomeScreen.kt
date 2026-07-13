package com.kimght.LimbusScreenTranslator.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.X
import com.kimght.LimbusScreenTranslator.core.designsystem.FlagChip
import com.kimght.LimbusScreenTranslator.core.designsystem.GoldButton
import com.kimght.LimbusScreenTranslator.core.designsystem.MarkdownChangelog
import com.kimght.LimbusScreenTranslator.core.designsystem.OutlineButton
import com.kimght.LimbusScreenTranslator.core.designsystem.SectionLabel
import com.kimght.LimbusScreenTranslator.ui.theme.Hairline
import com.kimght.LimbusScreenTranslator.ui.theme.HairlineStrong
import com.kimght.LimbusScreenTranslator.ui.theme.InsetBg
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus100
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus200
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus300
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus400
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus500

@Composable
fun HomeScreen(
    onOpenDetail: (sourceName: String, id: String) -> Unit,
    onBrowseLibrary: () -> Unit,
    onOpenOverlay: () -> Unit,
    onCloseOverlay: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onManage = { active -> onOpenDetail(active.sourceName, active.id) },
        onBrowseLibrary = onBrowseLibrary,
        onPrimaryAction = {
            val active = state.active
            when {
                state.overlayRunning -> onCloseOverlay()
                active != null && active.hasUpdate && !active.isInstalling ->
                    viewModel.updateActive(onDone = onOpenOverlay)

                else -> onOpenOverlay()
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onManage: (ActiveLocalization) -> Unit,
    onBrowseLibrary: () -> Unit,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp, bottom = 28.dp),
    ) {
        SectionLabel("Active Localization")
        androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))

        val active = state.active
        if (active != null) {
            ActiveCard(active)
            androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))

            val ctaLabel = when {
                state.overlayRunning -> "Close Overlay"
                active.isInstalling -> "Installing…"
                active.hasUpdate -> "Update & Open Overlay"
                else -> "Open Overlay"
            }
            GoldButton(
                text = ctaLabel,
                onClick = onPrimaryAction,
                enabled = state.overlayRunning || !active.isInstalling,
                leading = { color ->
                    Icon(
                        imageVector = if (state.overlayRunning) Lucide.X else Lucide.Play,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = color,
                    )
                },
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(9.dp))
            OutlineButton(text = "Manage localization", onClick = { onManage(active) })

            androidx.compose.foundation.layout.Spacer(Modifier.size(22.dp))
            val whatsNew = active.hasUpdate
            val changelog = if (whatsNew) {
                active.updateDescription?.takeIf { it.isNotBlank() } ?: active.description
            } else {
                active.description
            }
            SectionLabel(
                if (whatsNew) "What's New" else "Description",
                trailing = if (whatsNew) active.availableVersion else active.installedVersion,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
            if (changelog.isNotBlank()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (whatsNew) Limbus400.copy(alpha = 0.08f) else InsetBg)
                        .border(
                            1.dp,
                            if (whatsNew) Limbus400.copy(alpha = 0.40f) else Hairline,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                ) {
                    MarkdownChangelog(changelog)
                }
            }
        } else {
            EmptyState(onBrowseLibrary)
        }
    }
}

@Composable
private fun ActiveCard(active: ActiveLocalization) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, HairlineStrong, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            FlagChip(active.flag)
            Column(Modifier.weight(1f)) {
                Text(
                    text = active.name,
                    color = Limbus100,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${active.installedVersion} · via ${active.sourceName}",
                    color = Limbus500,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
        if (active.hasUpdate) {
            Row(
                modifier = Modifier
                    .padding(top = 13.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Limbus400.copy(alpha = 0.08f))
                    .border(1.dp, Limbus400.copy(alpha = 0.40f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "UPDATE",
                    color = Limbus400,
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${active.installedVersion}  →  ${active.availableVersion}",
                    color = Limbus200,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onBrowseLibrary: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No localization is active.\nInstall one and set it active.",
            color = Limbus500,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        OutlineButton(
            text = "Browse library",
            onClick = onBrowseLibrary,
            foreground = Limbus300,
            border = Limbus500,
            background = Limbus500.copy(alpha = 0.16f),
            modifier = Modifier.fillMaxWidth(0.6f),
        )
    }
}
