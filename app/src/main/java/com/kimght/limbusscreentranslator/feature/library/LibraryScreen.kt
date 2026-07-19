package com.kimght.limbusscreentranslator.feature.library

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Lucide
import com.kimght.limbusscreentranslator.R
import com.kimght.limbusscreentranslator.core.designsystem.FlagChip
import com.kimght.limbusscreentranslator.core.designsystem.SectionLabel
import com.kimght.limbusscreentranslator.core.designsystem.clickableEnabled
import com.kimght.limbusscreentranslator.data.repository.LocalizationListing
import com.kimght.limbusscreentranslator.domain.model.LocalizationStatus
import com.kimght.limbusscreentranslator.ui.theme.Hairline
import com.kimght.limbusscreentranslator.ui.theme.InsetBg
import com.kimght.limbusscreentranslator.ui.theme.Limbus100
import com.kimght.limbusscreentranslator.ui.theme.Limbus200
import com.kimght.limbusscreentranslator.ui.theme.Limbus300
import com.kimght.limbusscreentranslator.ui.theme.Limbus500

@Composable
fun LibraryScreen(
    onOpenDetail: (sourceName: String, id: String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryContent(
        state = state,
        onSelectSource = viewModel::selectSource,
        onRetry = viewModel::retry,
        onOpenDetail = { item ->
            state.selectedSource?.let {
                onOpenDetail(
                    it.name,
                    item.localization.id
                )
            }
        },
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

@Composable
private fun LibraryContent(
    state: LibraryUiState,
    onSelectSource: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenDetail: (LocalizationListing) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp),
    ) {
        if (state.noSources) {
            NoSourcesState(onOpenSettings)
        } else {
            SectionLabel(stringResource(R.string.library_source))
            Spacer(Modifier.size(9.dp))
            SourceSelector(
                sources = state.sources,
                selected = state.selectedSource,
                onSelect = onSelectSource,
            )
            Spacer(Modifier.size(18.dp))
            SectionLabel(
                stringResource(R.string.library_localizations, state.selectedSource?.name ?: ""),
            )
            Spacer(Modifier.size(10.dp))

            when {
                state.error -> ErrorState(onRetry)
                state.loading -> Text(
                    text = stringResource(R.string.library_loading),
                    color = Limbus500,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp),
                ) {
                    items(state.items, key = { it.localization.id }) { item ->
                        LocalizationRow(item, onOpen = onOpenDetail)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceSelector(
    sources: List<com.kimght.limbusscreentranslator.domain.model.Source>,
    selected: com.kimght.limbusscreentranslator.domain.model.Source?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp))
                .background(Limbus300.copy(alpha = 0.07f))
                .border(1.dp, Limbus300.copy(alpha = 0.40f), RoundedCornerShape(3.dp))
                .clickableEnabled(sources.isNotEmpty()) { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = selected?.name ?: stringResource(R.string.library_no_sources_selector),
                    color = Limbus300,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected != null) {
                    Text(
                        text = selected.host,
                        color = Limbus500,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Limbus500,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sources.forEach { source ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = source.name,
                                color = if (source == selected) Limbus300 else Limbus200,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                            )
                            Text(text = source.host, color = Limbus500, fontSize = 10.sp)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(source.name)
                    },
                )
            }
        }
    }
}

@Composable
private fun LocalizationRow(
    listing: LocalizationListing,
    onOpen: (LocalizationListing) -> Unit,
) {
    val loc = listing.localization
    val indicator = listing.status.rowIndicator()
    val borderColor =
        if (listing.status == LocalizationStatus.ACTIVE) {
            Limbus300.copy(alpha = 0.55f)
        } else {
            Hairline
        }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(InsetBg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickableEnabled(true) { onOpen(listing) }
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        FlagChip(loc.flag, width = 38.dp, height = 28.dp, fontSize = 10.sp)
        Column(Modifier.weight(1f)) {
            Text(
                text = loc.name,
                color = Limbus100,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${loc.version} · ${formatSize(loc.sizeBytes)}",
                color = Limbus500,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        if (indicator != null) {
            val label = stringResource(indicator.label)
            if (indicator.spinner) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp)
                        .semantics { contentDescription = label },
                    color = indicator.tint,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = indicator.icon!!,
                    contentDescription = label,
                    modifier = Modifier.size(18.dp),
                    tint = indicator.tint,
                )
            }
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Hairline, RoundedCornerShape(4.dp))
            .padding(horizontal = 18.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.library_error),
            color = Limbus500,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        com.kimght.limbusscreentranslator.core.designsystem.OutlineButton(
            text = stringResource(R.string.library_retry),
            onClick = onRetry,
            foreground = Limbus300,
            border = Limbus500,
            background = Limbus500.copy(alpha = 0.16f),
            modifier = Modifier.fillMaxWidth(0.5f),
        )
    }
}

@Composable
private fun NoSourcesState(onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Hairline, RoundedCornerShape(4.dp))
            .padding(horizontal = 18.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.library_empty),
            color = Limbus500,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        com.kimght.limbusscreentranslator.core.designsystem.OutlineButton(
            text = stringResource(R.string.library_open_settings),
            onClick = onOpenSettings,
            foreground = Limbus300,
            border = Limbus500,
            background = Limbus500.copy(alpha = 0.16f),
            modifier = Modifier.fillMaxWidth(0.5f),
        )
    }
}

internal fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val mb = bytes / 1_000_000.0
    return if (mb >= 1.0) "%.1f MB".format(mb) else "%.0f KB".format(bytes / 1000.0)
}
