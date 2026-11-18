package com.jaiho.sss.`in`.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.jaiho.sss.`in`.data.BotRuntime
import com.jaiho.sss.`in`.data.TargetResolver
import com.jaiho.sss.`in`.model.BotSettings
import com.jaiho.sss.`in`.model.NetStats
import com.jaiho.sss.`in`.model.SessionStats
import com.jaiho.sss.`in`.net.NetHookService
import com.jaiho.sss.`in`.service.BotEngine
import com.jaiho.sss.`in`.service.OverlayService
import com.jaiho.sss.`in`.util.L
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val overlay: Boolean = false,
    val tapService: Boolean = false,
    val notifications: Boolean = false,
    val installed: Boolean = false
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    val settings: StateFlow<BotSettings> = BotRuntime.settings
    val stats: StateFlow<SessionStats> = BotRuntime.stats
    val net: StateFlow<NetStats> = BotRuntime.net

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun refresh() {
        try {
            val ctx = getApplication<Application>()
            _state.value = HomeUiState(
                overlay = Permissions.canDrawOverlays(ctx),
                tapService = Permissions.tapServiceEnabled(ctx),
                notifications = Permissions.notificationsEnabled(ctx),
                installed = TargetResolver.gameInstalled(ctx, BotRuntime.settings.value.targetPackage)
            )
        } catch (t: Throwable) {
            L.e("refresh failed", t)
        }
    }

    fun showMessage(text: String) {
        _message.value = text
    }

    fun clearMessage() {
        _message.value = null
    }

    fun openJaiho() {
        val ctx = getApplication<Application>()
        try {
            val overlay = Permissions.canDrawOverlays(ctx)
            if (!overlay) showMessage("Allow \"Display over other apps\" to see the panel")

            BotRuntime.update { it.copy(panelHidden = false) }
            OverlayService.start(ctx)

            if (!Permissions.tapServiceEnabled(ctx)) {
                showMessage("Turn on the Tap service so the bot can press")
            }

            val target = TargetResolver.effectiveTarget(ctx, BotRuntime.settings.value.targetPackage)
            if (target.isEmpty()) {
                showMessage("Jaiho 777 not installed")
                return
            }
            val intent = TargetResolver.launchIntentFor(ctx, target)
            if (intent == null) {
                showMessage("Jaiho 777 not installed")
            } else {
                ctx.startActivity(intent)
            }
            if (BotRuntime.stats.value.spins == 0) BotRuntime.resetStats()
        } catch (t: Throwable) {
            L.e("open jaiho failed", t)
            showMessage("Could not open Jaiho 777")
        }
    }

    fun openStore() {
        try {
            getApplication<Application>().startActivity(TargetResolver.storeSearchIntent())
        } catch (t: Throwable) {
            L.e("store open failed", t)
            showMessage("Could not open the store")
        }
    }

    fun startAim() {
        val ctx = getApplication<Application>()
        if (!Permissions.canDrawOverlays(ctx)) {
            showMessage("Allow \"Display over other apps\" first")
            return
        }
        try {
            OverlayService.aim(ctx)
        } catch (t: Throwable) {
            L.e("aim failed", t)
        }
    }

    fun setSpeed(speed: Int) = BotRuntime.update { it.copy(speed = speed.coerceIn(1, 6)) }

    fun setOpacity(value: Float) = BotRuntime.update { it.copy(opacity = value.coerceIn(0.3f, 1f)) }

    fun setMultiplier(value: Float) = BotRuntime.update { it.copy(multiplier = value.coerceIn(1f, 20f)) }

    fun setNetHook(on: Boolean) = BotRuntime.update { it.copy(netHook = on) }

    fun resetMarkers() {
        BotRuntime.update { BotSettings.clearedMarkers(it) }
        showMessage("Tap points cleared — aim again")
    }

    fun stopBot() {
        val ctx = getApplication<Application>()
        try {
            BotRuntime.update { it.copy(alwaysWin = false, jackpot = false) }
            BotEngine.stopAll()
            NetHookService.requestStop(ctx)
            OverlayService.stop(ctx)
            showMessage("Bot stopped")
        } catch (t: Throwable) {
            L.e("stop bot failed", t)
        }
    }
}
