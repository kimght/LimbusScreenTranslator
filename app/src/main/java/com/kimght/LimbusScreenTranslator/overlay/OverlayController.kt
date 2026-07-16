package com.kimght.LimbusScreenTranslator.overlay

import com.kimght.LimbusScreenTranslator.data.datastore.Settings
import com.kimght.LimbusScreenTranslator.data.datastore.SettingsRepository
import com.kimght.LimbusScreenTranslator.data.repository.EpisodeUnavailableException
import com.kimght.LimbusScreenTranslator.data.repository.LocalizationRepository
import com.kimght.LimbusScreenTranslator.data.repository.OverlayStateRepository
import com.kimght.LimbusScreenTranslator.data.repository.ReadingState
import com.kimght.LimbusScreenTranslator.data.repository.ScenarioRepository
import com.kimght.LimbusScreenTranslator.domain.model.Chapter
import com.kimght.LimbusScreenTranslator.domain.model.DialogueLine
import com.kimght.LimbusScreenTranslator.domain.model.InstalledPack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class OverlayController(
    private val settings: SettingsRepository,
    private val localizations: LocalizationRepository,
    private val scenarios: ScenarioRepository,
    private val overlayState: OverlayStateRepository,
    private val scope: CoroutineScope,
) {
    private val mode = MutableStateFlow(OverlayMode.DIALOGUE)
    private val minimized = MutableStateFlow(false)
    private val resizeDraft = MutableStateFlow<OverlaySize?>(null)
    private var draftWidthDp = 0f
    private var draftHeightDp = 0f
    private val expandedChapter = MutableStateFlow<String?>(null)
    private val isPortrait = MutableStateFlow(false)

    private val activePack: Flow<InstalledPack?> = combine(
        settings.settings,
        localizations.installedPacks,
    ) { prefs, packs ->
        prefs.activeLocalizationId?.let { key -> packs.firstOrNull { it.key == key } }
    }.distinctUntilChanged()

    private val content: Flow<Content?> = activePack.flatMapLatest { pack ->
        if (pack == null) return@flatMapLatest flowOf(null)
        val chapters = scenarios.chapters(pack.sourceName)
        val reading = overlayState.readingState(pack.key)
        val lines = reading
            .map { it.currentEpisode }
            .distinctUntilChanged()
            .map { episode -> loadLines(pack.key, episode) }
        combine(reading, lines) { state, load ->
            Content(pack, chapters, state, load.lines, load.unavailable)
        }
    }

    private data class LocalState(
        val mode: OverlayMode,
        val minimized: Boolean,
        val draft: OverlaySize?,
        val expanded: String?,
    )

    private val local: Flow<LocalState> =
        combine(mode, minimized, resizeDraft, expandedChapter) { m, min, draft, exp ->
            LocalState(m, min, draft, exp)
        }

    val uiState: StateFlow<OverlayUiState> = combine(
        settings.settings,
        local,
        content,
        isPortrait,
    ) { prefs, local, content, portrait ->
        val width = local.draft?.width ?: prefs.overlayWidth
        val contentHeight = local.draft?.contentHeight ?: prefs.overlayContentHeight
        if (content == null) {
            OverlayUiState(
                present = true,
                noActiveLocalization = true,
                mode = local.mode,
                minimized = local.minimized,
                portrait = portrait,
                opacity = prefs.opacity,
                textScale = prefs.textSize,
                overlayWidth = width,
                overlayContentHeight = contentHeight,
            )
        } else {
            val reading = content.reading
            val model = chapterModelFor(
                ChapterKey(
                    chapters = content.chapters,
                    currentEpisode = reading.currentEpisode,
                    viewed = reading.viewedEpisodes,
                    expanded = local.expanded,
                    sourceName = content.pack.sourceName,
                ),
            )
            OverlayUiState(
                present = true,
                mode = local.mode,
                minimized = local.minimized,
                portrait = portrait,
                opacity = prefs.opacity,
                textScale = prefs.textSize,
                overlayWidth = width,
                overlayContentHeight = contentHeight,
                episodeKey = reading.currentEpisode,
                lines = content.lines,
                lineIndex = reading.lineIndex.coerceIn(
                    0,
                    (content.lines.size - 1).coerceAtLeast(0)
                ),
                episodeUnavailable = content.unavailable,
                chapters = model.rows,
                prevEpisode = model.nav.prev,
                nextEpisode = model.nav.next,
                totalEpisodes = model.totalEpisodes,
                chapterSourceName = model.sourceName,
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, OverlayUiState())

    private data class ChapterKey(
        val chapters: List<Chapter>,
        val currentEpisode: String?,
        val viewed: Set<String>,
        val expanded: String?,
        val sourceName: String,
    )

    private data class ChapterModel(
        val rows: List<ChapterRow>,
        val nav: EpisodeNav,
        val totalEpisodes: Int,
        val sourceName: String?,
    )

    private var cachedChapterKey: ChapterKey? = null
    private var cachedChapterModel: ChapterModel? = null

    private fun chapterModelFor(key: ChapterKey): ChapterModel {
        cachedChapterModel?.takeIf { key == cachedChapterKey }?.let { return it }
        val expanded = key.expanded ?: defaultExpandedChapter(key.chapters, key.currentEpisode)
        return ChapterModel(
            rows = buildChapterRows(key.chapters, key.currentEpisode, key.viewed, expanded),
            nav = episodeNav(key.chapters, key.currentEpisode),
            totalEpisodes = key.chapters.sumOf { it.episodes.size },
            sourceName = key.sourceName,
        ).also {
            cachedChapterKey = key
            cachedChapterModel = it
        }
    }

    fun selectMode(target: OverlayMode) {
        if (mode.value == target) return
        if (target == OverlayMode.RESIZE) {
            scope.launch {
                val prefs = settings.settings.first()
                startResizeDraft(prefs.overlayWidth, prefs.overlayContentHeight)
                mode.value = OverlayMode.RESIZE
            }
        } else {
            leaveResizeTo(target)
        }
    }

    fun minimize() {
        minimized.value = true
        if (mode.value == OverlayMode.RESIZE) leaveResizeTo(OverlayMode.DIALOGUE)
    }

    fun restore() {
        if (!isPortrait.value) minimized.value = false
    }

    fun setOrientation(portrait: Boolean) {
        isPortrait.value = portrait
        if (portrait) {
            minimized.value = true
            if (mode.value == OverlayMode.RESIZE) leaveResizeTo(OverlayMode.DIALOGUE)
        }
    }

    fun updateResizeDraft(dWidthDp: Float, dHeightDp: Float) {
        if (resizeDraft.value == null) return
        draftWidthDp = (draftWidthDp + dWidthDp).coerceIn(
            Settings.MIN_OVERLAY_WIDTH.toFloat(),
            Settings.MAX_OVERLAY_WIDTH.toFloat(),
        )
        draftHeightDp = (draftHeightDp + dHeightDp).coerceIn(
            Settings.MIN_OVERLAY_CONTENT_HEIGHT.toFloat(),
            Settings.MAX_OVERLAY_CONTENT_HEIGHT.toFloat(),
        )
        resizeDraft.value = OverlaySize(draftWidthDp.roundToInt(), draftHeightDp.roundToInt())
    }

    private fun startResizeDraft(width: Int, contentHeight: Int) {
        draftWidthDp = width.toFloat()
        draftHeightDp = contentHeight.toFloat()
        resizeDraft.value = OverlaySize(width, contentHeight)
    }

    fun persistResizeDraft() {
        val draft = resizeDraft.value ?: return
        scope.launch { settings.setOverlaySize(draft.width, draft.contentHeight) }
    }

    private fun leaveResizeTo(target: OverlayMode) {
        mode.value = target
        val draft = resizeDraft.value ?: return
        scope.launch {
            settings.setOverlaySize(draft.width, draft.contentHeight)
            settings.settings.first {
                it.overlayWidth == draft.width && it.overlayContentHeight == draft.contentHeight
            }
            resizeDraft.value = null
        }
    }

    fun resetSize() {
        startResizeDraft(Settings.DEFAULT_OVERLAY_WIDTH, Settings.DEFAULT_OVERLAY_CONTENT_HEIGHT)
        scope.launch {
            settings.setOverlaySize(
                Settings.DEFAULT_OVERLAY_WIDTH,
                Settings.DEFAULT_OVERLAY_CONTENT_HEIGHT
            )
        }
    }

    fun toggleChapterExpand(chapterKey: String) {
        val current = uiState.value.chapters.firstOrNull { it.expanded }?.key
        expandedChapter.value = if (current == chapterKey) "" else chapterKey
    }

    fun selectEpisode(episodeCode: String) {
        scope.launch {
            val id = activeId() ?: return@launch
            overlayState.selectEpisode(id, episodeCode)
            mode.value = OverlayMode.DIALOGUE
        }
    }

    fun setLineIndex(index: Int) {
        scope.launch {
            val id = activeId() ?: return@launch
            overlayState.setLineIndex(id, index)
        }
    }

    fun setPanelPosition(x: Int, y: Int) {
        scope.launch { settings.setPanelPosition(x, y) }
    }

    fun setMinimizedPositionFromService(x: Int, y: Int) {
        scope.launch { settings.setMinimizedPosition(x, y) }
    }

    private suspend fun activeId(): String? = settings.settings.first().activeLocalizationId

    private suspend fun loadLines(id: String, episode: String?): LineLoad {
        if (episode == null) return LineLoad(emptyList(), unavailable = false)
        return try {
            LineLoad(scenarios.loadEpisode(id, episode), unavailable = false)
        } catch (_: EpisodeUnavailableException) {
            LineLoad(emptyList(), unavailable = true)
        }
    }

    private data class Content(
        val pack: InstalledPack,
        val chapters: List<Chapter>,
        val reading: ReadingState,
        val lines: List<DialogueLine>,
        val unavailable: Boolean,
    )

    private data class LineLoad(val lines: List<DialogueLine>, val unavailable: Boolean)
}
