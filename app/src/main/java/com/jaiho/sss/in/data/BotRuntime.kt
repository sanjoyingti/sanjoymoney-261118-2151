package com.jaiho.sss.`in`.data

import com.jaiho.sss.`in`.model.BotSettings
import com.jaiho.sss.`in`.model.NetStats
import com.jaiho.sss.`in`.model.SessionStats
import com.jaiho.sss.`in`.util.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Live state shared by the home screen, the floating panel and the services.
 * Writing goes to the in-memory flow first (instant UI) and is then persisted.
 */
object BotRuntime {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var store: SettingsStore? = null

    val settings = MutableStateFlow(BotSettings())
    val stats = MutableStateFlow(SessionStats())
    val net = MutableStateFlow(NetStats())

    /** -1 = AIM mode closed, otherwise index into TAP_POINTS. */
    val aimStep = MutableStateFlow(-1)

    fun attach(store: SettingsStore) {
        if (this.store === store) return
        this.store = store
        scope.launch {
            runCatching {
                store.settings.collect { loaded -> settings.value = loaded }
            }.onFailure { L.e("settings collect failed", it) }
        }
    }

    fun update(block: (BotSettings) -> BotSettings) {
        val next = block(settings.value)
        settings.value = next
        val target = store ?: return
        scope.launch {
            runCatching { target.save(next) }.onFailure { L.e("settings save failed", it) }
        }
    }

    fun resetStats() {
        stats.value = SessionStats(startedAt = System.currentTimeMillis())
    }

    fun bumpTap(fired: Boolean) {
        stats.update {
            it.copy(
                spins = it.spins + 1,
                streak = if (fired) it.streak + 1 else 0,
                misses = if (fired) it.misses else it.misses + 1
            )
        }
    }

    fun bumpNet(bytes: Long, frames: Long) {
        net.update { it.copy(bytes = it.bytes + bytes, frames = it.frames + frames) }
    }
}
