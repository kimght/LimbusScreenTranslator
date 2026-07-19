package com.kimght.limbusscreentranslator.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import androidx.core.view.WindowInsetsCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kimght.limbusscreentranslator.MainActivity
import com.kimght.limbusscreentranslator.R
import com.kimght.limbusscreentranslator.core.i18n.ProvideUiLanguage
import com.kimght.limbusscreentranslator.core.i18n.localizedTo
import com.kimght.limbusscreentranslator.data.datastore.Settings as AppSettings
import com.kimght.limbusscreentranslator.data.datastore.SettingsRepository
import com.kimght.limbusscreentranslator.data.repository.LocalizationRepository
import com.kimght.limbusscreentranslator.data.repository.OverlayStateRepository
import com.kimght.limbusscreentranslator.data.repository.ScenarioRepository
import com.kimght.limbusscreentranslator.overlay.ui.OVERLAY_CHROME_DP
import com.kimght.limbusscreentranslator.overlay.ui.OVERLAY_MINIMIZED_SIZE_DP
import com.kimght.limbusscreentranslator.overlay.ui.OverlayActions
import com.kimght.limbusscreentranslator.overlay.ui.OverlayRoot
import com.kimght.limbusscreentranslator.ui.theme.LimbusScreenTranslatorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class OverlayService : Service() {

    @Inject
    lateinit var settings: SettingsRepository
    @Inject
    lateinit var localizations: LocalizationRepository
    @Inject
    lateinit var scenarios: ScenarioRepository
    @Inject
    lateinit var overlayState: OverlayStateRepository
    @Inject
    lateinit var runningState: OverlayRunningState

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var composeView: ComposeView? = null
    private var host: OverlayWindowHost? = null
    @Volatile
    private var minimizedNow: Boolean = false
    private var controllerRef: OverlayController? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        runningState.set(true)
        startForegroundNotification()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val controller = OverlayController(settings, localizations, scenarios, overlayState, scope)
        controllerRef = controller
        controller.setOrientation(isPortrait())
        addOverlayView(controller)
        scope.launch {
            settings.settings.map { it.uiLanguage }.distinctUntilChanged().collect { lang ->
                val localized = localizedTo(lang)
                ensureChannel(localized)
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification(localized))
            }
        }
        scope.launch {
            var lastMinimized: Boolean? = null
            var lastWidth = -1
            var lastHeight = -1
            controller.uiState.collect { ui ->
                val minimizedChanged = ui.minimized != lastMinimized
                val sizeChanged =
                    ui.overlayWidth != lastWidth || ui.overlayContentHeight != lastHeight
                lastWidth = ui.overlayWidth
                lastHeight = ui.overlayContentHeight
                if (minimizedChanged) {
                    lastMinimized = ui.minimized
                    minimizedNow = ui.minimized
                    applyWindowState(ui)
                } else if (!ui.minimized && sizeChanged) {
                    applySize(ui)
                    clampNow()
                    updateWindow()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        runningState.set(false)
        composeView?.let { runCatching { windowManager.removeView(it) } }
        composeView = null
        host?.destroy()
        host = null
        scope.cancel()
        super.onDestroy()
    }

    private fun addOverlayView(controller: OverlayController) {
        val host = OverlayWindowHost().also { it.create() }
        this.host = host

        layoutParams = overlayWindowLayoutParams()

        val actions = OverlayActions(
            onSelectMode = controller::selectMode,
            onMinimize = controller::minimize,
            onRestore = controller::restore,
            onDismiss = { stopSelf() },
            onResizeDrag = controller::updateResizeDraft,
            onResizeEnd = controller::persistResizeDraft,
            onResetSize = controller::resetSize,
            onSelectEpisode = controller::selectEpisode,
            onToggleChapterExpand = controller::toggleChapterExpand,
            onLineSettled = controller::setLineIndex,
            onDrag = ::onDragBy,
            onDragEnd = ::onDragEnd,
        )

        val view = ComposeView(this).apply {
            host.attachTo(this)
            setContent {
                val languageFlow = remember {
                    settings.settings
                        .map { it.uiLanguage }
                        .distinctUntilChanged()
                }
                val language by languageFlow.collectAsStateWithLifecycle(initialValue = AppSettings.defaultUiLanguage())
                LimbusScreenTranslatorTheme {
                    ProvideUiLanguage(language) {
                        OverlayRoot(stateFlow = controller.uiState, actions = actions)
                    }
                }
            }
        }
        composeView = view
        windowManager.addView(view, layoutParams)
    }

    private fun onDragBy(dx: Float, dy: Float) {
        layoutParams.x += dx.roundToInt()
        layoutParams.y += dy.roundToInt()
        clampNow()
        updateWindow()
    }

    private fun onDragEnd() {
        clampNow()
        updateWindow()
        val size = viewSizePx(minimizedNow)
        val center = topLeftToNaturalCenter(
            layoutParams.x, layoutParams.y, size.x, size.y, displayFrame(),
        )
        if (minimizedNow) controllerRef?.setMinimizedPositionFromService(center.x, center.y)
        else controllerRef?.setPanelPosition(center.x, center.y)
    }

    private fun viewSizePx(minimized: Boolean): Point {
        if (!minimized) return Point(layoutParams.width, layoutParams.height)
        val side = (OVERLAY_MINIMIZED_SIZE_DP * resources.displayMetrics.density).roundToInt()
        return Point(side, side)
    }

    private fun clampNow() {
        val v = composeView ?: return
        val screen = realDisplaySize()
        val w = if (layoutParams.width > 0) layoutParams.width
        else if (v.width > 0) v.width else v.measuredWidth
        val h = if (layoutParams.height > 0) layoutParams.height
        else if (v.height > 0) v.height else v.measuredHeight
        val clamped = clampToScreen(
            layoutParams.x, layoutParams.y, w, h, screen.x, screen.y, statusBarInsets(),
        )
        layoutParams.x = clamped.x
        layoutParams.y = clamped.y
    }

    private fun statusBarInsets(): ScreenInsets {
        val view = composeView ?: return ScreenInsets.NONE
        val root = view.rootWindowInsets ?: return ScreenInsets.NONE
        val insets = WindowInsetsCompat.toWindowInsetsCompat(root, view)
            .getInsets(WindowInsetsCompat.Type.statusBars())
        return ScreenInsets(insets.left, insets.top, insets.right, insets.bottom)
    }

    private fun displayFrame(): DisplayFrame {
        val size = realDisplaySize()
        val rotation = getSystemService(DisplayManager::class.java)
            ?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
        return DisplayFrame(rotation, size.x, size.y)
    }

    private fun realDisplaySize(): Point =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            @Suppress("DEPRECATION")
            Point().also { windowManager.defaultDisplay.getRealSize(it) }
        }

    private suspend fun applyWindowState(ui: OverlayUiState) {
        val prefs = settings.settings.first()
        applySize(ui)
        val x = if (ui.minimized) prefs.minimizedX else prefs.panelX
        val y = if (ui.minimized) prefs.minimizedY else prefs.panelY
        if (x != null && y != null) {
            val size = viewSizePx(ui.minimized)
            val topLeft = naturalCenterToTopLeft(
                ScreenPosition(x, y), size.x, size.y, displayFrame(),
            )
            layoutParams.x = topLeft.x
            layoutParams.y = topLeft.y
        } else {
            defaultPlacement()
        }
        clampNow()
        updateWindow()
    }

    private fun applySize(ui: OverlayUiState) {
        if (ui.minimized) {
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            val density = resources.displayMetrics.density
            layoutParams.width = (ui.overlayWidth * density).roundToInt()
            layoutParams.height =
                ((ui.overlayContentHeight + OVERLAY_CHROME_DP) * density).roundToInt()
        }
    }

    private fun updateWindow() {
        composeView?.let { runCatching { windowManager.updateViewLayout(it, layoutParams) } }
    }

    private fun defaultPlacement() {
        layoutParams.x = (12 * resources.displayMetrics.density).roundToInt()
        layoutParams.y = (realDisplaySize().y * 0.52f).roundToInt()
    }

    private fun isPortrait(): Boolean =
        resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        controllerRef?.setOrientation(
            newConfig.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT,
        )
        // Stored positions are natural-frame, so re-applying them lands the
        // overlay at the same physical spot in the new orientation.
        composeView?.post {
            val controller = controllerRef ?: return@post
            scope.launch { applyWindowState(controller.uiState.value) }
        }
    }

    private fun startForegroundNotification() {
        ensureChannel(this)
        val type = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(this), type)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.overlay_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(context: Context): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.overlay_notification_title))
            .setContentText(context.getString(R.string.overlay_notification_text))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 0x10B

        fun start(context: android.content.Context) {
            val intent = Intent(context, OverlayService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
