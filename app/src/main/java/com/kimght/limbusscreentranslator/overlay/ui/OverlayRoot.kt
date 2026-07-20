package com.kimght.limbusscreentranslator.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Captions
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.List
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize
import com.composables.icons.lucide.Minimize
import com.composables.icons.lucide.RotateCwSquare
import com.composables.icons.lucide.X
import com.kimght.limbusscreentranslator.R
import com.kimght.limbusscreentranslator.overlay.EpisodeMarker
import com.kimght.limbusscreentranslator.overlay.EpisodeShortcut
import com.kimght.limbusscreentranslator.overlay.OverlayMode
import com.kimght.limbusscreentranslator.overlay.OverlayUiState
import com.kimght.limbusscreentranslator.data.datastore.Settings
import com.kimght.limbusscreentranslator.domain.markup.DialogueMarkup
import com.kimght.limbusscreentranslator.overlay.lineNumberLabel
import com.kimght.limbusscreentranslator.overlay.overlaySizeLabel
import com.kimght.limbusscreentranslator.ui.theme.BgBackground
import com.kimght.limbusscreentranslator.ui.theme.Danger
import com.kimght.limbusscreentranslator.ui.theme.Hairline
import com.kimght.limbusscreentranslator.ui.theme.InsetBg
import com.kimght.limbusscreentranslator.ui.theme.Limbus50
import com.kimght.limbusscreentranslator.ui.theme.Limbus100
import com.kimght.limbusscreentranslator.ui.theme.Limbus200
import com.kimght.limbusscreentranslator.ui.theme.Limbus300
import com.kimght.limbusscreentranslator.ui.theme.Limbus500
import com.kimght.limbusscreentranslator.ui.theme.Limbus600
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class OverlayActions(
    val onSelectMode: (OverlayMode) -> Unit,
    val onMinimize: () -> Unit,
    val onRestore: () -> Unit,
    val onDismiss: () -> Unit,
    val onResizeDrag: (Float, Float) -> Unit,
    val onResizeEnd: () -> Unit,
    val onResetSize: () -> Unit,
    val onSelectEpisode: (String) -> Unit,
    val onToggleChapterExpand: (String) -> Unit,
    val onLineSettled: (Int) -> Unit,
    val onDrag: (Float, Float) -> Unit,
    val onDragEnd: () -> Unit,
    val onRetryChapters: () -> Unit,
)

private val PanelBorder = Limbus300.copy(alpha = 0.34f)
private const val HEADER_HEIGHT_DP = 34
private val HeaderHeight = HEADER_HEIGHT_DP.dp
internal const val OVERLAY_CHROME_DP = HEADER_HEIGHT_DP + 8 + 10
private val OverlayChrome = OVERLAY_CHROME_DP.dp
private const val MINIMIZED_ICON_DP = 46
private const val MINIMIZED_PADDING_DP = 6
// The WRAP_CONTENT window size of MinimizedIcon; the service uses it for
// center-anchored positioning before the bubble has been laid out.
internal const val OVERLAY_MINIMIZED_SIZE_DP = MINIMIZED_ICON_DP + 2 * MINIMIZED_PADDING_DP

@Composable
fun OverlayRoot(stateFlow: StateFlow<OverlayUiState>, actions: OverlayActions) {
    val state by stateFlow.collectAsStateWithLifecycle()
    if (!state.present) return
    if (state.minimized) {
        MinimizedIcon(state, actions)
    } else {
        OverlayPanel(state, actions)
    }
}

@Composable
private fun OverlayPanel(state: OverlayUiState, actions: OverlayActions) {
    val panelWidth =
        state.overlayWidth.coerceIn(Settings.MIN_OVERLAY_WIDTH, Settings.MAX_OVERLAY_WIDTH).dp
    val panelHeight = state.overlayContentHeight.dp + OverlayChrome

    Row(
        Modifier
            .alpha(state.opacity.coerceIn(0.2f, 1f))
            .width(panelWidth)
            .height(panelHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(BgBackground.copy(alpha = 0.93f))
            .border(
                1.dp,
                if (state.resizing) Limbus300.copy(alpha = 0.55f) else PanelBorder,
                RoundedCornerShape(8.dp),
            ),
    ) {
        Box(Modifier
            .weight(1f)
            .fillMaxHeight()) {
            when (state.mode) {
                OverlayMode.RESIZE -> ResizeContent(state, actions, Modifier.fillMaxSize())
                OverlayMode.DIALOGUE -> DialogueContent(state, actions, Modifier.fillMaxSize())
                OverlayMode.CHAPTER -> ChapterContent(state, actions, Modifier.fillMaxSize())
            }
            if (state.resizing) {
                ResetButton(actions, Modifier.align(Alignment.BottomStart))
                ResizeGrip(actions, Modifier.align(Alignment.BottomEnd))
            }
        }
        Box(Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(Limbus300.copy(alpha = 0.2f)))
        Rail(state, actions, Modifier.fillMaxHeight())
    }
}

@Composable
private fun DialogueContent(state: OverlayUiState, actions: OverlayActions, modifier: Modifier) {
    Column(modifier.padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 10.dp)) {
        if (state.lines.isEmpty()) {
            DialogueHeader(speaker = null, title = null, actions = actions)
            Box(Modifier
                .weight(1f)
                .fillMaxWidth(), contentAlignment = Alignment.Center) {
                Placeholder(stringResource(emptyDialogueMessageRes(state)))
            }
            return@Column
        }

        val lastIndex = state.lines.lastIndex
        val pagerState = rememberPagerState(
            initialPage = state.lineIndex.coerceIn(0, lastIndex),
        ) { state.lines.size }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.distinctUntilChanged()
                .collect(actions.onLineSettled)
        }
        LaunchedEffect(state.episodeKey, state.lines.size) {
            val target = state.lineIndex.coerceIn(0, state.lines.lastIndex)
            if (pagerState.currentPage != target) pagerState.scrollToPage(target)
        }

        val current = state.lines[pagerState.currentPage.coerceIn(0, lastIndex)]
        DialogueHeader(current.speakerName, current.title, actions)

        val scope = rememberCoroutineScope()
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(lastIndex) {
                    detectTapGestures {
                        val next = pagerState.currentPage + 1
                        if (next <= lastIndex) scope.launch { pagerState.animateScrollToPage(next) }
                    }
                },
        ) {
            VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                LineText(state.lines[page].text, state.textScale)
            }
            Text(
                text = lineNumberLabel(pagerState.currentPage),
                color = Limbus300.copy(alpha = 0.15f),
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 2.dp),
            )
        }
    }
}

@Composable
private fun DialogueHeader(speaker: String?, title: String?, actions: OverlayActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeaderHeight)
            .dragHandle(actions)
            .padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!speaker.isNullOrBlank()) {
            Text(
                text = speaker.uppercase(),
                color = Limbus300,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!title.isNullOrBlank()) {
            Text(
                text = title.uppercase(),
                color = Limbus500,
                fontSize = 10.sp,
                letterSpacing = 1.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Limbus300.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                ),
        )
    }
}

@Composable
private fun LineText(text: String, scale: Float) {
    val styled = remember(text) { DialogueMarkup.parse(text).toAnnotatedString() }
    Box(Modifier
        .fillMaxSize()
        .padding(horizontal = 26.dp), contentAlignment = Alignment.Center) {
        Text(
            text = styled,
            color = Limbus100,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 1.sp,
                maxFontSize = (18f * scale).sp,
                stepSize = 0.25.sp,
            ),
            lineHeight = 1.44.em,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterContent(
    state: OverlayUiState,
    actions: OverlayActions,
    modifier: Modifier,
) {
    Column(modifier.padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .dragHandle(actions)
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.overlay_chapter_select),
                color = Limbus300,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.sp
            )
            val episodesLabel = pluralStringResource(
                R.plurals.overlay_episode_count, state.totalEpisodes, state.totalEpisodes,
            )
            Text(
                state.chapterSourceName?.takeIf { it.isNotBlank() }
                    ?.let { "$episodesLabel · $it" } ?: episodesLabel,
                color = Limbus500, fontSize = 10.sp, letterSpacing = 1.4.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }

        if (state.chapters.isEmpty()) {
            Box(Modifier
                .weight(1f)
                .fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (state.noActiveLocalization) {
                    Placeholder(stringResource(R.string.overlay_no_active))
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Placeholder(stringResource(R.string.overlay_chapters_unavailable))
                        RetryChaptersButton(
                            syncing = state.chapterSyncing,
                            onRetry = actions.onRetryChapters,
                        )
                    }
                }
            }
            return@Column
        }

        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item("quick-nav") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NavButton(
                        stringResource(R.string.overlay_prev),
                        state.prevEpisode,
                        actions.onSelectEpisode,
                        Modifier.weight(1f)
                    )
                    NavButton(
                        stringResource(R.string.overlay_next),
                        state.nextEpisode,
                        actions.onSelectEpisode,
                        Modifier.weight(1f)
                    )
                }
            }
            items(state.chapters, key = { it.key }) { chapter ->
                ChapterSection(chapter, actions.onToggleChapterExpand, actions.onSelectEpisode)
            }
        }
    }
}

@Composable
private fun NavButton(
    label: String,
    shortcut: EpisodeShortcut?,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = shortcut != null
    Column(
        modifier
            .clip(RoundedCornerShape(2.dp))
            .background(if (enabled) Limbus300.copy(alpha = 0.07f) else Color(0x18000000))
            .border(
                1.dp,
                if (enabled) Limbus300.copy(alpha = 0.4f) else Hairline,
                RoundedCornerShape(2.dp),
            )
            .then(if (shortcut != null) Modifier.clickable { onClick(shortcut.code) } else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label,
            color = if (enabled) Limbus300 else Limbus600,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            shortcut?.let {
                "${it.meta} · ${stringResource(R.string.overlay_episode_short, it.episodeNumber)}"
            } ?: "—",
            color = if (enabled) Limbus200 else Limbus600,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RetryChaptersButton(syncing: Boolean, onRetry: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Limbus300.copy(alpha = 0.07f))
            .border(
                1.dp,
                if (syncing) Hairline else Limbus300.copy(alpha = 0.4f),
                RoundedCornerShape(2.dp),
            )
            .then(if (syncing) Modifier else Modifier.clickable(onClick = onRetry))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(if (syncing) R.string.overlay_retrying else R.string.overlay_retry),
            color = if (syncing) Limbus600 else Limbus300,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterSection(
    chapter: com.kimght.limbusscreentranslator.overlay.ChapterRow,
    onToggle: (String) -> Unit,
    onSelect: (String) -> Unit,
) {
    Column(Modifier.padding(horizontal = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .background(if (chapter.expanded) Limbus300.copy(alpha = 0.08f) else InsetBg)
                .border(
                    1.dp,
                    Limbus300.copy(alpha = if (chapter.expanded) 0.4f else 0.18f),
                    RoundedCornerShape(2.dp)
                )
                .clickable { onToggle(chapter.key) }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                chapter.name,
                color = Limbus500,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.2.sp
            )
            Text(
                chapter.subtitle,
                color = Limbus200,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(chapter.progressLabel, color = Limbus600, fontSize = 10.sp, letterSpacing = 0.6.sp)
            Chevron(expanded = chapter.expanded)
        }
        if (chapter.expanded) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                chapter.episodes.forEach { EpisodeButton(it, onSelect) }
            }
        }
    }
}

@Composable
private fun Chevron(expanded: Boolean) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Icon(
        Lucide.ChevronDown,
        null,
        Modifier
            .size(14.dp)
            .graphicsLayer { rotationZ = rotation },
        tint = Limbus300.copy(alpha = 0.55f),
    )
}

@Composable
private fun EpisodeButton(
    cell: com.kimght.limbusscreentranslator.overlay.EpisodeCell,
    onSelect: (String) -> Unit
) {
    val bg: Color
    val barColor: Color
    val fg: Color
    val statusIcon: ImageVector?
    val statusColor: Color
    when (cell.marker) {
        EpisodeMarker.NOW_PLAYING -> {
            bg = Limbus300.copy(alpha = 0.14f); barColor = Limbus300; fg = Limbus100
            statusIcon = null; statusColor = Limbus300
        }

        EpisodeMarker.VIEWED -> {
            bg = Limbus50.copy(alpha = 0.015f); barColor = Limbus300.copy(alpha = 0.16f); fg =
                Limbus500
            statusIcon = Lucide.Check; statusColor = Limbus500
        }

        EpisodeMarker.NONE -> {
            bg = InsetBg; barColor = Limbus300.copy(alpha = 0.08f); fg = Limbus200
            statusIcon = null; statusColor = Color.Transparent
        }
    }
    Box(
        Modifier
            .width(44.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(bg)
            .border(1.dp, barColor, RoundedCornerShape(2.dp))
            .clickable { onSelect(cell.code) },
        contentAlignment = Alignment.Center,
    ) {
        Text(cell.shortLabel, color = fg, fontSize = 12.sp, maxLines = 1)
        if (statusIcon != null) {
            Icon(
                statusIcon,
                null,
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(9.dp),
                tint = statusColor,
            )
        }
    }
}

@Composable
private fun Rail(state: OverlayUiState, actions: OverlayActions, modifier: Modifier) {
    Column(
        modifier
            .width(50.dp)
            .background(InsetBg)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RailButton(onClick = actions.onMinimize) {
            Icon(Lucide.Minimize, null, Modifier.size(18.dp), tint = Danger)
        }
        Spacer(Modifier.weight(1f))
        SectionButton(Lucide.Captions, OverlayMode.DIALOGUE, state.mode, actions.onSelectMode)
        SectionButton(Lucide.List, OverlayMode.CHAPTER, state.mode, actions.onSelectMode)
        SectionButton(Lucide.Maximize, OverlayMode.RESIZE, state.mode, actions.onSelectMode)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SectionButton(
    icon: ImageVector,
    target: OverlayMode,
    current: OverlayMode,
    onSelect: (OverlayMode) -> Unit,
) {
    val active = target == current
    RailButton(onClick = { onSelect(target) }) {
        if (active) {
            Box(
                Modifier
                    .size(34.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Limbus300.copy(alpha = 0.25f), Color.Transparent),
                        ),
                    ),
            )
        }
        Icon(
            icon,
            null,
            Modifier.size(20.dp),
            tint = if (active) Limbus300 else Limbus500,
        )
    }
}

@Composable
private fun RailButton(
    onClick: () -> Unit,
    danger: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val base = Modifier
        .size(width = 40.dp, height = 38.dp)
        .clip(RoundedCornerShape(3.dp))
    val decorated = if (danger) {
        base
            .background(Danger.copy(alpha = 0.30f))
            .border(1.dp, Danger.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
    } else {
        base
    }
    Box(
        decorated.clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun ResizeContent(state: OverlayUiState, actions: OverlayActions, modifier: Modifier) {
    Column(modifier.padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(HeaderHeight)
                .dragHandle(actions)
                .padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.overlay_resize),
                color = Limbus300,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 2.sp
            )
            Spacer(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Limbus300.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    ),
            )
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .dragHandle(actions),
            contentAlignment = Alignment.Center,
        ) {
            CornerBrackets(Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp))
            Text(
                text = overlaySizeLabel(state.overlayWidth, state.overlayContentHeight),
                color = Limbus100,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                letterSpacing = 1.5.sp,
            )
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun CornerBrackets(modifier: Modifier) {
    val color = Limbus300.copy(alpha = 0.4f)
    androidx.compose.foundation.Canvas(modifier) {
        val len = minOf(size.width, size.height) * 0.16f
        val sw = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        // top-left
        drawLine(color, Offset(0f, 0f), Offset(len, 0f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(0f, 0f), Offset(0f, len), sw, cap = StrokeCap.Round)
        // top-right
        drawLine(color, Offset(w, 0f), Offset(w - len, 0f), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w, 0f), Offset(w, len), sw, cap = StrokeCap.Round)
        // bottom-left
        drawLine(color, Offset(0f, h), Offset(len, h), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(0f, h), Offset(0f, h - len), sw, cap = StrokeCap.Round)
        // bottom-right
        drawLine(color, Offset(w, h), Offset(w - len, h), sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w, h), Offset(w, h - len), sw, cap = StrokeCap.Round)
    }
}

@Composable
private fun ResizeGrip(actions: OverlayActions, modifier: Modifier) {
    Box(
        modifier
            .padding(4.dp)
            .size(32.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(BgBackground.copy(alpha = 0.97f))
            .border(1.dp, Limbus300, RoundedCornerShape(5.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = actions.onResizeEnd,
                    onDragCancel = actions.onResizeEnd,
                ) { change, drag ->
                    change.consume()
                    actions.onResizeDrag(drag.x.toDp().value, drag.y.toDp().value)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Lucide.Maximize, null, Modifier.size(16.dp), tint = Limbus300)
    }
}

@Composable
private fun ResetButton(actions: OverlayActions, modifier: Modifier) {
    Box(
        modifier
            .padding(4.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(5.dp))
            .border(1.dp, Hairline, RoundedCornerShape(5.dp))
            .background(Color(0x73000000))
            .clickable(onClick = actions.onResetSize)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.overlay_reset), color = Limbus500, fontSize = 10.sp, letterSpacing = 0.8.sp)
    }
}

private const val ROTATE_HINT_MILLIS = 1500L

@Composable
private fun MinimizedIcon(state: OverlayUiState, actions: OverlayActions) {
    var hintNonce by remember { mutableIntStateOf(0) }
    var showRotateHint by remember { mutableStateOf(false) }
    LaunchedEffect(hintNonce, state.portrait) {
        showRotateHint = state.portrait && hintNonce > 0
        if (showRotateHint) {
            delay(ROTATE_HINT_MILLIS.milliseconds)
            showRotateHint = false
        }
        hintNonce = 0
    }
    val active = !state.portrait
    Box(Modifier.padding(MINIMIZED_PADDING_DP.dp)) {
        Box(
            Modifier
                .size(MINIMIZED_ICON_DP.dp)
                .clip(CircleShape)
                .background(BgBackground.copy(alpha = 0.95f))
                .border(1.dp, Limbus300.copy(alpha = if (active) 0.55f else 0.25f), CircleShape)
                .dragHandle(actions)
                .clickable { if (active) actions.onRestore() else hintNonce++ },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(showRotateHint, label = "minimized-icon") { hint ->
                if (hint) {
                    Icon(Lucide.RotateCwSquare, null, Modifier.size(22.dp), tint = Limbus300)
                } else {
                    Icon(
                        Lucide.Captions,
                        null,
                        Modifier.size(22.dp),
                        tint = if (active) Limbus300 else Limbus300.copy(alpha = 0.4f),
                    )
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(17.dp)
                .clip(CircleShape)
                .background(Danger)
                .border(1.5.dp, BgBackground, CircleShape)
                .clickable(onClick = actions.onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Lucide.X, null, Modifier.size(9.dp), tint = Color.White)
        }
    }
}

@Composable
private fun Placeholder(message: String) {
    Text(
        message,
        color = Limbus500,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
}

@StringRes
private fun emptyDialogueMessageRes(state: OverlayUiState): Int = when {
    state.noActiveLocalization -> R.string.overlay_no_active
    state.episodeUnavailable -> R.string.overlay_episode_unavailable
    else -> R.string.overlay_pick_episode
}

private fun Modifier.dragHandle(actions: OverlayActions): Modifier = pointerInput(Unit) {
    detectDragGestures(
        onDragEnd = actions.onDragEnd,
        onDragCancel = actions.onDragEnd,
    ) { change, dragAmount ->
        change.consume()
        actions.onDrag(dragAmount.x, dragAmount.y)
    }
}
