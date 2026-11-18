package com.jaiho.sss.`in`.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import com.jaiho.sss.`in`.data.BotRuntime
import com.jaiho.sss.`in`.model.TAP_POINTS
import com.jaiho.sss.`in`.model.TapPoint
import com.jaiho.sss.`in`.ui.overlay.AimOverlayScreen
import com.jaiho.sss.`in`.ui.overlay.OverlayOwner
import com.jaiho.sss.`in`.ui.overlay.PanelContent
import com.jaiho.sss.`in`.ui.overlay.createOverlayView
import com.jaiho.sss.`in`.util.L
import kotlinx.coroutines.flow.MutableStateFlow

object OverlayHost {

    private var windowManager: WindowManager? = null

    private var panelView: ComposeView? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var panelOwner: OverlayOwner? = null

    private var aimView: ComposeView? = null
    private var aimOwner: OverlayOwner? = null

    private val marker = MutableStateFlow(Offset(0.5f, 0.55f))

    private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

    val panelShown: Boolean get() = panelView != null

    /** Runs a window task on the main thread, on the spot when we are already there. */
    private fun onMain(block: () -> Unit) {
        val run = {
            try {
                block()
            } catch (t: Throwable) {
                L.e("overlay window task failed", t)
            }
        }
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                run()
            } else {
                mainHandler.post { run() }
            }
        } catch (t: Throwable) {
            L.e("overlay main post failed", t)
        }
    }

    fun init(context: Context) {
        if (windowManager == null) {
            windowManager = context.getSystemService(WindowManager::class.java)
        }
    }

    private fun savedMarker(kind: TapPoint): Offset {
        val point = BotRuntime.settings.value.point(kind)
        return if (point != null) Offset(point.first, point.second) else Offset(0.5f, 0.55f)
    }

    private fun panelLayoutParams(): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        val saved = BotRuntime.settings.value
        params.x = if (saved.panelX >= 0) saved.panelX else 40
        params.y = if (saved.panelY >= 0) saved.panelY else 260
        return params
    }

    fun showPanel(context: Context) = onMain { showPanelNow(context) }

    private fun showPanelNow(context: Context) {
        if (panelView != null) return
        try {
            val wm = windowManager ?: return
            val owner = OverlayOwner()
            owner.create()
            val params = panelLayoutParams()
            val view = context.createOverlayView(owner) {
                PanelContent(
                    onToggleWin = { BotRuntime.update { s -> s.copy(alwaysWin = it) } },
                    onToggleJackpot = { BotRuntime.update { s -> s.copy(jackpot = it) } },
                    onBet = { kind -> BotEngine.fireOnce(kind) },
                    onSpeed = { delta -> BotRuntime.update { s -> s.changingSpeed(delta) } },
                    onTap = { BotEngine.fireOnce(TapPoint.SPIN) },
                    onAim = { startAim(context) },
                    onCollapse = { collapsed -> BotRuntime.update { s -> s.copy(collapsed = collapsed) } },
                    onHide = { hidePanel(context) },
                    onDrag = { dx, dy -> dragPanel(dx, dy) },
                    onDragEnd = { savePosition() }
                )
            }
            wm.addView(view, params)
            panelView = view
            panelParams = params
            panelOwner = owner
            L.d("panel attached")
        } catch (t: Throwable) {
            L.e("panel attach failed", t)
            panelOwner?.destroy()
            panelOwner = null
            panelView = null
            panelParams = null
        }
    }

    private fun dragPanel(dx: Float, dy: Float) {
        try {
            val wm = windowManager ?: return
            val params = panelParams ?: return
            val view = panelView ?: return
            val maxX = (Screen.width - view.width - 8).coerceAtLeast(0)
            val maxY = (Screen.height - view.height - 8).coerceAtLeast(0)
            params.x = (params.x + dx.toInt()).coerceIn(0, maxX)
            params.y = (params.y + dy.toInt()).coerceIn(0, maxY)
            wm.updateViewLayout(view, params)
        } catch (t: Throwable) {
            L.e("panel drag failed", t)
        }
    }

    private fun savePosition() {
        val params = panelParams ?: return
        BotRuntime.update { it.copy(panelX = params.x, panelY = params.y) }
    }

    fun hidePanel(context: Context) = onMain { hidePanelNow() }

    private fun hidePanelNow() {
        try {
            val view = panelView ?: return
            windowManager?.removeView(view)
        } catch (t: Throwable) {
            L.e("panel detach failed", t)
        } finally {
            panelOwner?.destroy()
            panelOwner = null
            panelView = null
            panelParams = null
        }
    }

    fun startAim(context: Context) = onMain {
        BotRuntime.aimStep.value = 0
        showAimNow(context)
    }

    fun showAim(context: Context) = onMain { showAimNow(context) }

    private fun showAimNow(context: Context) {
        if (aimView != null) return
        val step = BotRuntime.aimStep.value
        if (step < 0) return
        val kind = TAP_POINTS.getOrNull(step) ?: return
        marker.value = savedMarker(kind)
        try {
            val wm = windowManager ?: return
            val owner = OverlayOwner()
            owner.create()
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            val view = context.createOverlayView(owner) {
                val stepState by BotRuntime.aimStep.collectAsState()
                val markerState by marker.collectAsState()
                if (stepState >= 0) {
                    AimOverlayScreen(
                        step = stepState,
                        marker = markerState,
                        onMarker = { marker.value = it },
                        onConfirm = { confirmAim(context) },
                        onCancel = { endAim(context) }
                    )
                }
            }
            wm.addView(view, params)
            aimView = view
            aimOwner = owner
            L.d("aim overlay attached")
        } catch (t: Throwable) {
            L.e("aim attach failed", t)
            aimOwner?.destroy()
            aimOwner = null
            aimView = null
            BotRuntime.aimStep.value = -1
        }
    }

    private fun confirmAim(context: Context) {
        try {
            val step = BotRuntime.aimStep.value
            val kind = TAP_POINTS.getOrNull(step) ?: run { endAim(context); return }
            val position = marker.value
            BotRuntime.update { it.withPoint(kind, position.x, position.y) }
            val next = step + 1
            if (next >= TAP_POINTS.size) {
                endAim(context)
            } else {
                BotRuntime.aimStep.value = next
                marker.value = savedMarker(TAP_POINTS[next])
            }
        } catch (t: Throwable) {
            L.e("confirm aim failed", t)
            endAim(context)
        }
    }

    fun endAim(context: Context) = onMain { endAimNow() }

    private fun endAimNow() {
        BotRuntime.aimStep.value = -1
        try {
            val view = aimView
            if (view != null) windowManager?.removeView(view)
        } catch (t: Throwable) {
            L.e("aim detach failed", t)
        } finally {
            aimOwner?.destroy()
            aimOwner = null
            aimView = null
            BotEngine.sync()
        }
    }

    fun hideEverything(context: Context) = onMain {
        endAimNow()
        hidePanelNow()
    }
}
