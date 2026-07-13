package com.kimght.LimbusScreenTranslator.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.kimght.LimbusScreenTranslator.MainActivity
import com.kimght.LimbusScreenTranslator.R
import com.kimght.LimbusScreenTranslator.data.datastore.SettingsRepository
import com.kimght.LimbusScreenTranslator.data.repository.LocalizationRepository
import com.kimght.LimbusScreenTranslator.data.repository.OverlayStateRepository
import com.kimght.LimbusScreenTranslator.data.repository.ScenarioRepository
import com.kimght.LimbusScreenTranslator.overlay.ui.OVERLAY_CHROME_DP
import com.kimght.LimbusScreenTranslator.overlay.ui.OverlayActions
import com.kimght.LimbusScreenTranslator.overlay.ui.OverlayRoot
import com.kimght.LimbusScreenTranslator.ui.theme.LimbusScreenTranslatorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
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
            var lastMinimized: Boolean? = null
            var lastWidth = -1
            var lastHeight = -1
            controller.uiState.collect { ui ->
                val minimizedChanged = ui.minimized != lastMinimized
                val sizeChanged =
                    ui.overlayWidth != lastWidth || ui.overlayContentHeight != lastHeight
                lastWidth = ui.overlayWidth
                lastHeight = ui.overlayContentHeight
                if (minimizedChanged || (!ui.minimized && sizeChanged)) applyWindowSize(ui)
                if (minimizedChanged) {
                    lastMinimized = ui.minimized
                    minimizedNow = ui.minimized
                    applyPositionForState()
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
                LimbusScreenTranslatorTheme {
                    OverlayRoot(stateFlow = controller.uiState, actions = actions)
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
        composeView?.let { runCatching { windowManager.updateViewLayout(it, layoutParams) } }
    }

    private fun onDragEnd() {
        clampNow()
        composeView?.let { runCatching { windowManager.updateViewLayout(it, layoutParams) } }
        if (minimizedNow) controllerRef?.setMinimizedPositionFromService(
            layoutParams.x,
            layoutParams.y
        )
        else controllerRef?.setPanelPosition(layoutParams.x, layoutParams.y)
    }

    private fun clampNow() {
        val v = composeView ?: return
        val screen = realDisplaySize()
        val w = if (v.width > 0) v.width else v.measuredWidth
        val h = if (v.height > 0) v.height else v.measuredHeight
        val clamped = clampToScreen(layoutParams.x, layoutParams.y, w, h, screen.x, screen.y)
        layoutParams.x = clamped.x
        layoutParams.y = clamped.y
    }

    private fun realDisplaySize(): Point =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            @Suppress("DEPRECATION")
            Point().also { windowManager.defaultDisplay.getRealSize(it) }
        }

    private fun applyWindowSize(ui: OverlayUiState) {
        val view = composeView ?: return
        if (ui.minimized) {
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            val density = resources.displayMetrics.density
            val screen = realDisplaySize()
            val widthPx = (ui.overlayWidth * density).roundToInt()
            val heightPx = ((ui.overlayContentHeight + OVERLAY_CHROME_DP) * density).roundToInt()
            layoutParams.width = widthPx
            layoutParams.height = heightPx
            val clamped = clampToScreen(
                layoutParams.x, layoutParams.y, widthPx, heightPx, screen.x, screen.y,
            )
            layoutParams.x = clamped.x
            layoutParams.y = clamped.y
        }
        runCatching { windowManager.updateViewLayout(view, layoutParams) }
    }

    private fun applyPositionForState() {
        scope.launch {
            val prefs = settings.settings.first()
            if (minimizedNow) {
                layoutParams.x = prefs.minimizedX
                layoutParams.y = prefs.minimizedY
                if (prefs.minimizedX == 0 && prefs.minimizedY == 0) defaultPlacement()
            } else {
                if (prefs.panelX != 0 || prefs.panelY != 0) {
                    layoutParams.x = prefs.panelX
                    layoutParams.y = prefs.panelY
                } else {
                    defaultPlacement()
                }
            }
            clampNow()
            composeView?.let { runCatching { windowManager.updateViewLayout(it, layoutParams) } }
        }
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
        composeView?.post {
            clampNow()
            composeView?.let { runCatching { windowManager.updateViewLayout(it, layoutParams) } }
            if (minimizedNow) controllerRef?.setMinimizedPositionFromService(
                layoutParams.x,
                layoutParams.y
            )
            else controllerRef?.setPanelPosition(layoutParams.x, layoutParams.y)
        }
    }

    private fun startForegroundNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) }
            manager.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        val type = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
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
