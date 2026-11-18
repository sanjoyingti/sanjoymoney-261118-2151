=== ABOUT: The tap loops: it presses SPIN and JACKPOT over and over with a random gap so it never looks robotic ===
package com.jaiho.sss.in.service

import com.jaiho.sss.in.data.BotRuntime
import com.jaiho.sss.in.model.TapPoint
import com.jaiho.sss.in.model.jitteredInterval
import com.jaiho.sss.in.util.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object BotEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var winJob: Job? = null
    private var jackpotJob: Job? = null

    @Volatile
    private var paused = false

    @Volatile
    private var winWanted = false

    @Volatile
    private var jackpotWanted = false

    /** Brings the running loops in line with the switches and the saved points. */
    fun sync() {
        val s = BotRuntime.settings.value
        drive(TapPoint.SPIN, s.alwaysWin, winWanted) { winWanted = it }
        drive(TapPoint.JACKPOT, s.jackpot, jackpotWanted) { jackpotWanted = it }
    }

    private fun drive(kind: TapPoint, wanted: Boolean, running: Boolean, store: (Boolean) -> Unit) {
        if (wanted == running) return
        store(wanted)
        if (kind == TapPoint.SPIN) {
            if (wanted) startSpinLoop() else stopSpinLoop()
        } else {
            if (wanted) startJackpotLoop() else stopJackpotLoop()
        }
    }

    fun setAlwaysWin(on: Boolean) {
        winWanted = on
        if (on) startSpinLoop() else stopSpinLoop()
    }

    fun setJackpot(on: Boolean) {
        jackpotWanted = on
        if (on) startJackpotLoop() else stopJackpotLoop()
    }

    private fun startSpinLoop() {
        if (winJob?.isActive == true) return
        winJob = scope.launch { loop(TapPoint.SPIN) }
    }

    private fun stopSpinLoop() {
        winJob?.cancel()
        winJob = null
    }

    private fun startJackpotLoop() {
        if (jackpotJob?.isActive == true) return
        jackpotJob = scope.launch { loop(TapPoint.JACKPOT) }
    }

    private fun stopJackpotLoop() {
        jackpotJob?.cancel()
        jackpotJob = null
    }

    private suspend fun loop(kind: TapPoint) {
        try {
            while (scope.isActive) {
                val before = BotRuntime.settings.value
                val point = before.point(kind)
                if (point == null) {
                    delay(400)
                    continue
                }
                delay(jitteredInterval(before.speed))
                if (paused) continue
                val fresh = BotRuntime.settings.value.point(kind) ?: continue
                fire(fresh.first, fresh.second)
            }
        } catch (t: Throwable) {
            L.e("loop $kind died", t)
        }
    }

    /** One manual press at the saved point of the given kind. */
    fun fireOnce(kind: TapPoint) {
        val point = BotRuntime.settings.value.point(kind) ?: return
        fire(point.first, point.second)
    }

    private fun fire(nx: Float, ny: Float) {
        try {
            val (x, y) = Screen.px(nx, ny)
            val ok = TapAccessibilityService.tap(x, y, 60L)
            BotRuntime.bumpTap(ok)
        } catch (t: Throwable) {
            L.e("fire failed", t)
        }
    }

    fun pause() {
        paused = true
        L.d("loops paused")
    }

    fun resume() {
        paused = false
        L.d("loops resumed")
    }

    fun stopAll() {
        winWanted = false
        jackpotWanted = false
        stopSpinLoop()
        stopJackpotLoop()
    }
}
